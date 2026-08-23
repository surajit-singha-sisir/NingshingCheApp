package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.Article
import com.example.data.model.ArticleComment
import com.example.data.model.Author
import com.example.data.model.Bookmark
import com.example.data.model.Category
import com.example.data.model.PdfCategory
import com.example.data.model.PdfDocument
import com.example.data.model.ReadingHistory
import com.example.data.model.YearArchive
import com.example.data.remote.NingshingCheWebsiteClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WebsiteSyncState(
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long = 0L,
    val lastMessage: String = "",
    val liveArticleCount: Int = 0,
    val usingLiveSite: Boolean = false
)

class ArticleRepository(
    private val database: AppDatabase,
    private val websiteClient: NingshingCheWebsiteClient = NingshingCheWebsiteClient()
) {

    private val articleDao = database.articleDao()
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val searchDao = database.searchDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _categories = MutableStateFlow(NinghsingCheContentData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _authors = MutableStateFlow(NinghsingCheContentData.authors)
    val authors: StateFlow<List<Author>> = _authors.asStateFlow()

    private val _yearArchives = MutableStateFlow(NinghsingCheContentData.yearArchives)
    val yearArchives: StateFlow<List<YearArchive>> = _yearArchives.asStateFlow()

    private val _pdfDocuments = MutableStateFlow(NinghsingCheContentData.pdfDocuments)
    val pdfDocuments: StateFlow<List<PdfDocument>> = _pdfDocuments.asStateFlow()

    private val _pdfCategories = MutableStateFlow(NinghsingCheContentData.pdfCategories)
    val pdfCategories: StateFlow<List<PdfCategory>> = _pdfCategories.asStateFlow()

    private val _syncState = MutableStateFlow(WebsiteSyncState())
    val syncState: StateFlow<WebsiteSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            val existing = articleDao.getAllArticles().first()
            if (existing.isEmpty()) {
                seedInitialArticles()
            } else {
                rebuildCatalogsFrom(existing.map { it.toModel() })
            }
            syncFromWebsite()
        }
    }

    suspend fun seedInitialArticles() {
        val entities = NinghsingCheContentData.articles.map { it.toEntity() }
        articleDao.insertArticles(entities)
    }

    suspend fun syncFromWebsite(): Result<Int> = syncMutex.withLock {
        _syncState.value = _syncState.value.copy(isSyncing = true, lastMessage = "নিংশিংচে.কম থেকে হালনাগাদ হচ্ছে...")
        val result = websiteClient.syncCatalog()
        result.fold(
            onSuccess = { listing ->
                if (listing.articles.isNotEmpty()) {
                    upsertLiveArticles(listing.articles)
                    articleDao.deleteSeedArticles()
                    _categories.value = listing.categories.ifEmpty { buildCategories(listing.articles) }
                    _authors.value = listing.authors.ifEmpty { NingshingCheWebsiteClient.buildAuthors(listing.articles) }
                    _yearArchives.value = buildYearArchives(listing.articles)
                }
                if (listing.pdfDocuments.isNotEmpty()) {
                    _pdfDocuments.value = listing.pdfDocuments
                    _pdfCategories.value = buildPdfCategories(listing.pdfDocuments)
                }
                prefetchFeaturedBodies(listing.articles)
                _syncState.value = WebsiteSyncState(
                    isSyncing = false,
                    lastSuccessAt = System.currentTimeMillis(),
                    lastMessage = "নিংশিংচে.কম থেকে ${listing.articles.size}টি প্রবন্ধ হালনাগাদ হয়েছে",
                    liveArticleCount = listing.articles.size,
                    usingLiveSite = listing.articles.isNotEmpty()
                )
                Result.success(listing.articles.size)
            },
            onFailure = { error ->
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastMessage = "সিঙ্ক ব্যর্থ: ${error.message ?: "নেটওয়ার্ক ত্রুটি"}। অফলাইন আর্কাইভ দেখানো হচ্ছে।"
                )
                Result.failure(error)
            }
        )
    }

    fun refreshInBackground() {
        scope.launch { syncFromWebsite() }
    }

    fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
            val live = entities.filter { !it.id.startsWith("art-") }
            when {
                live.isNotEmpty() -> live.map { it.toModel() }
                entities.isNotEmpty() -> entities.map { it.toModel() }
                else -> NinghsingCheContentData.articles
            }
        }
    }

    fun getFeaturedArticles(): Flow<List<Article>> {
        return articleDao.getFeaturedArticles().map { entities ->
            val live = entities.filter { !it.id.startsWith("art-") }
            when {
                live.isNotEmpty() -> live.map { it.toModel() }
                entities.isNotEmpty() -> entities.map { it.toModel() }
                else -> NinghsingCheContentData.articles.filter { it.isFeatured }
            }
        }
    }

    suspend fun getArticleById(idOrSlug: String): Article? {
        val key = normalizeKey(idOrSlug)
        val entity = articleDao.getArticleByIdOrSlug(key)
            ?: articleDao.getArticleByIdOrSlug(idOrSlug)
            ?: articleDao.getArticleByIdOrSlug(idOrSlug.removeSuffix(".kehem"))

        val local = entity?.toModel()
            ?: NinghsingCheContentData.articles.find { it.id == key || it.slug == key }

        val dirty = local != null && NingshingCheWebsiteClient.looksDirty(local.content)
        val needsBody = local == null || local.content.isBlank() || local.content.length < 80 || dirty
        if (needsBody) {
            val remote = websiteClient.fetchArticle(local?.sourceUrl?.takeIf { it.contains(".kehem") } ?: idOrSlug)
                ?: local?.sourceUrl?.let { websiteClient.fetchArticle(it) }
            if (remote != null) {
                val merged = mergeArticle(local, remote)
                articleDao.insertArticle(merged.toEntity())
                return merged
            }
        }
        return local
    }

    fun getArticlesByCategory(categorySlug: String): Flow<List<Article>> {
        return articleDao.getArticlesByCategory(categorySlug).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.categorySlug == categorySlug } }
        }
    }

    fun getArticlesByAuthor(authorId: String): Flow<List<Article>> {
        return articleDao.getArticlesByAuthor(authorId).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.authorId == authorId } }
        }
    }

    fun getArticlesByYear(year: Int): Flow<List<Article>> {
        return articleDao.getArticlesByYear(year).map { entities ->
            val mapped = entities.map { it.toModel() }
            mapped.ifEmpty { NinghsingCheContentData.articles.filter { it.year == year } }
        }
    }

    fun searchArticles(query: String, categorySlug: String? = null, year: Int? = null): Flow<List<Article>> {
        val trimmed = query.trim()
        return flow {
            val all = if (trimmed.isEmpty()) {
                articleDao.getAllArticles().first().map { it.toModel() }
                    .ifEmpty { NinghsingCheContentData.articles }
            } else {
                val dbResults = articleDao.searchArticles(trimmed).first().map { it.toModel() }
                dbResults.ifEmpty {
                    NinghsingCheContentData.articles.filter {
                        it.title.contains(trimmed, ignoreCase = true) ||
                            it.content.contains(trimmed, ignoreCase = true) ||
                            it.authorName.contains(trimmed, ignoreCase = true) ||
                            it.category.contains(trimmed, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(trimmed, ignoreCase = true) }
                    }
                }
            }

            emit(
                all.filter { article ->
                    val matchCategory = categorySlug == null || article.categorySlug == categorySlug
                    val matchYear = year == null || article.year == year
                    matchCategory && matchYear
                }
            )
        }
    }

    suspend fun loadComments(articleUrl: String): List<ArticleComment> =
        websiteClient.loadComments(articleUrl)

    suspend fun submitBlog(
        name: String,
        facebook: String,
        address: String,
        email: String,
        phone: String,
        writerInfo: String,
        articleTitle: String,
        articleBody: String,
        photoBytes: ByteArray?,
        photoName: String,
        fileBytes: ByteArray?,
        fileName: String
    ): Result<String> = websiteClient.submitBlog(
        name, facebook, address, email, phone, writerInfo, articleTitle, articleBody,
        photoBytes, photoName, fileBytes, fileName
    )

    suspend fun submitComment(
        articleUrl: String,
        name: String,
        address: String,
        email: String,
        phone: String,
        content: String
    ): Result<String> = websiteClient.submitComment(articleUrl, name, address, email, phone, content)

    fun getCategories(): List<Category> = _categories.value

    fun getCategoryBySlug(slug: String): Category? =
        _categories.value.find { it.slug == slug } ?: NinghsingCheContentData.categories.find { it.slug == slug }

    fun getAuthors(): List<Author> = _authors.value

    fun getAuthorById(id: String): Author? =
        _authors.value.find { it.id == id } ?: NinghsingCheContentData.authors.find { it.id == id }

    fun getYearArchives(): List<YearArchive> = _yearArchives.value

    fun getYearArchiveByYear(year: Int): YearArchive? =
        _yearArchives.value.find { it.year == year } ?: NinghsingCheContentData.yearArchives.find { it.year == year }

    fun getPdfCategories(): List<PdfCategory> = _pdfCategories.value

    fun getPdfDocuments(): List<PdfDocument> = _pdfDocuments.value

    fun getPdfDocumentById(id: String): PdfDocument? = _pdfDocuments.value.find { it.id == id }

    fun getPdfDocumentsByCategory(categoryIdOrSlug: String): List<PdfDocument> {
        if (categoryIdOrSlug == "pdf-cat-all" || categoryIdOrSlug.isBlank()) return _pdfDocuments.value
        return _pdfDocuments.value.filter { it.categorySlug == categoryIdOrSlug || it.category == categoryIdOrSlug }
    }

    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { Bookmark(it.articleId, it.savedAtTimestamp, it.folder, it.note) }
        }
    }

    fun isBookmarked(articleId: String): Flow<Boolean> = bookmarkDao.isBookmarked(articleId)

    suspend fun toggleBookmark(articleId: String) {
        val exists = bookmarkDao.isBookmarkedDirect(articleId)
        if (exists) {
            bookmarkDao.deleteBookmark(articleId)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(articleId = articleId))
        }
    }

    fun getReadingHistory(): Flow<List<ReadingHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { ReadingHistory(it.articleId, it.readAtTimestamp, it.scrollPosition, it.progressPercent) }
        }
    }

    suspend fun saveReadingProgress(articleId: String, scrollPos: Int, progress: Float) {
        historyDao.insertHistory(
            HistoryEntity(
                articleId = articleId,
                readAtTimestamp = System.currentTimeMillis(),
                scrollPosition = scrollPos,
                progressPercent = progress
            )
        )
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    fun getRecentSearches(): Flow<List<String>> {
        return searchDao.getRecentSearches().map { entities ->
            entities.map { it.query }
        }
    }

    suspend fun recordSearch(query: String) {
        if (query.isNotBlank()) {
            searchDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun removeSearch(query: String) {
        searchDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() {
        searchDao.clearAll()
    }

    suspend fun clearAllCache() {
        articleDao.clearAll()
        seedInitialArticles()
        _categories.value = NinghsingCheContentData.categories
        _authors.value = NinghsingCheContentData.authors
        _yearArchives.value = NinghsingCheContentData.yearArchives
        _pdfDocuments.value = NinghsingCheContentData.pdfDocuments
        _pdfCategories.value = NinghsingCheContentData.pdfCategories
        syncFromWebsite()
    }

    private suspend fun upsertLiveArticles(articles: List<Article>) {
        val existing = articleDao.getAllArticles().first().associateBy { it.id }
        val entities = articles.map { incoming ->
            val previous = existing[incoming.id] ?: existing.values.find { it.slug == incoming.slug }
            val previousDirty = previous != null && NingshingCheWebsiteClient.looksDirty(previous.content)
            if (previous != null && !previousDirty && previous.content.length > incoming.content.length) {
                incoming.copy(
                    content = previous.content,
                    excerpt = incoming.excerpt.ifBlank { previous.excerpt },
                    featuredImageUrl = incoming.featuredImageUrl.ifBlank { previous.featuredImageUrl },
                    authorAvatarUrl = incoming.authorAvatarUrl.ifBlank { previous.authorAvatarUrl }
                ).toEntity()
            } else {
                incoming.toEntity()
            }
        }
        articleDao.insertArticles(entities)
    }

    private suspend fun prefetchFeaturedBodies(articles: List<Article>) = coroutineScope {
        articles.filter { it.isFeatured || it.isEditorialPick }
            .distinctBy { it.id }
            .take(8)
            .map { article ->
                async {
                    if (article.content.length >= 80) return@async
                    val remote = websiteClient.fetchArticle(article.sourceUrl) ?: return@async
                    articleDao.insertArticle(mergeArticle(article, remote).toEntity())
                }
            }.awaitAll()
    }

    private fun mergeArticle(local: Article?, remote: Article): Article {
        if (local == null) return remote
        return remote.copy(
            isFeatured = local.isFeatured || remote.isFeatured,
            isEditorialPick = local.isEditorialPick || remote.isEditorialPick,
            category = remote.category.ifBlank { local.category },
            categorySlug = remote.categorySlug.ifBlank { local.categorySlug },
            authorName = remote.authorName.ifBlank { local.authorName },
            authorAvatarUrl = remote.authorAvatarUrl.ifBlank { local.authorAvatarUrl },
            featuredImageUrl = remote.featuredImageUrl.ifBlank { local.featuredImageUrl },
            excerpt = remote.excerpt.ifBlank { local.excerpt },
            content = remote.content.ifBlank { local.content },
            viewCount = maxOf(local.viewCount, remote.viewCount)
        )
    }

    private fun rebuildCatalogsFrom(articles: List<Article>) {
        val live = articles.filter { !it.id.startsWith("art-") }
        if (live.isEmpty()) return
        _categories.value = NingshingCheWebsiteClient.buildCategories(live)
        _authors.value = NingshingCheWebsiteClient.buildAuthors(live)
        _yearArchives.value = buildYearArchives(live)
        _syncState.value = _syncState.value.copy(
            liveArticleCount = live.size,
            usingLiveSite = true,
            lastMessage = "অফলাইন ক্যাশে ${live.size}টি লাইভ প্রবন্ধ"
        )
    }

    private fun buildCategories(articles: List<Article>): List<Category> =
        NingshingCheWebsiteClient.buildCategories(articles)

    private fun buildYearArchives(articles: List<Article>): List<YearArchive> {
        return articles.groupBy { it.year }
            .toSortedMap(compareByDescending { it })
            .map { (year, list) ->
                val bn = NingshingCheWebsiteClient.toBengaliDigits(year)
                YearArchive(
                    year = year,
                    bengaliYearText = bn,
                    title = "নিংশিং চে — $bn",
                    description = "নিংশিংচে.কম-এ $bn সালে প্রকাশিত প্রবন্ধ ও সংখ্যা।",
                    issueCount = 1,
                    articleCount = list.size
                )
            }
    }

    private fun buildPdfCategories(docs: List<PdfDocument>): List<PdfCategory> {
        val groups = docs.groupBy { it.categorySlug }
        return listOf(
            PdfCategory("pdf-cat-all", "সকল PDF", "নিংশিং চে-র সকল ডিজিটাল প্রকাশনা।", docs.size)
        ) + groups.map { (slug, list) ->
            PdfCategory(slug, list.first().category, list.first().category, list.size)
        }
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim()
            .removePrefix("https://ningshingche.com/")
            .removePrefix("http://ningshingche.com/")
            .removePrefix("/")
            .removeSuffix(".kehem")
            .substringAfterLast('/')
    }

    private fun Article.toEntity(): ArticleEntity {
        return ArticleEntity(
            id = id,
            title = title,
            slug = slug,
            excerpt = excerpt,
            content = content,
            featuredImageUrl = featuredImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            category = category,
            categorySlug = categorySlug,
            tagsRaw = tags.joinToString(","),
            publishedDate = publishedDate,
            year = year,
            readingTimeMinutes = readingTimeMinutes,
            isFeatured = isFeatured,
            isEditorialPick = isEditorialPick,
            viewCount = viewCount,
            sourceUrl = sourceUrl,
            relatedArticleIdsRaw = relatedArticleIds.joinToString(",")
        )
    }

    private fun ArticleEntity.toModel(): Article {
        return Article(
            id = id,
            title = title,
            slug = slug,
            excerpt = excerpt,
            content = content,
            featuredImageUrl = featuredImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            category = category,
            categorySlug = categorySlug,
            tags = if (tagsRaw.isNotBlank()) tagsRaw.split(",").map { it.trim() } else emptyList(),
            publishedDate = publishedDate,
            year = year,
            readingTimeMinutes = readingTimeMinutes,
            isFeatured = isFeatured,
            isEditorialPick = isEditorialPick,
            viewCount = viewCount,
            sourceUrl = sourceUrl,
            relatedArticleIds = if (relatedArticleIdsRaw.isNotBlank()) relatedArticleIdsRaw.split(",").map { it.trim() } else emptyList()
        )
    }
}
