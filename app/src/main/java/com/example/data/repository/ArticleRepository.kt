package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.Article
import com.example.data.model.Author
import com.example.data.model.Bookmark
import com.example.data.model.Category
import com.example.data.model.PdfCategory
import com.example.data.model.PdfDocument
import com.example.data.model.ReadingHistory
import com.example.data.model.YearArchive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ArticleRepository(private val database: AppDatabase) {

    private val articleDao = database.articleDao()
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val searchDao = database.searchDao()

    init {
        // Prepopulate database with rich default cultural content if empty
        CoroutineScope(Dispatchers.IO).launch {
            val existing = articleDao.getAllArticles().first()
            if (existing.isEmpty()) {
                seedInitialArticles()
            }
        }
    }

    suspend fun seedInitialArticles() {
        val entities = NinghsingCheContentData.articles.map { it.toEntity() }
        articleDao.insertArticles(entities)
    }

    fun getAllArticles(): Flow<List<Article>> {
        return articleDao.getAllArticles().map { entities ->
            if (entities.isEmpty()) {
                NinghsingCheContentData.articles
            } else {
                entities.map { it.toModel() }
            }
        }
    }

    fun getFeaturedArticles(): Flow<List<Article>> {
        return articleDao.getFeaturedArticles().map { entities ->
            if (entities.isEmpty()) {
                NinghsingCheContentData.articles.filter { it.isFeatured }
            } else {
                entities.map { it.toModel() }
            }
        }
    }

    suspend fun getArticleById(idOrSlug: String): Article? {
        val entity = articleDao.getArticleByIdOrSlug(idOrSlug)
        return entity?.toModel() ?: NinghsingCheContentData.articles.find { it.id == idOrSlug || it.slug == idOrSlug }
    }

    fun getArticlesByCategory(categorySlug: String): Flow<List<Article>> {
        return articleDao.getArticlesByCategory(categorySlug).map { entities ->
            if (entities.isEmpty()) {
                NinghsingCheContentData.articles.filter { it.categorySlug == categorySlug }
            } else {
                entities.map { it.toModel() }
            }
        }
    }

    fun getArticlesByAuthor(authorId: String): Flow<List<Article>> {
        return articleDao.getArticlesByAuthor(authorId).map { entities ->
            if (entities.isEmpty()) {
                NinghsingCheContentData.articles.filter { it.authorId == authorId }
            } else {
                entities.map { it.toModel() }
            }
        }
    }

    fun getArticlesByYear(year: Int): Flow<List<Article>> {
        return articleDao.getArticlesByYear(year).map { entities ->
            if (entities.isEmpty()) {
                NinghsingCheContentData.articles.filter { it.year == year }
            } else {
                entities.map { it.toModel() }
            }
        }
    }

    fun searchArticles(query: String, categorySlug: String? = null, year: Int? = null): Flow<List<Article>> {
        val trimmed = query.trim()
        return flow {
            val all = if (trimmed.isEmpty()) {
                articleDao.getAllArticles().first().map { it.toModel() }.ifEmpty { NinghsingCheContentData.articles }
            } else {
                val dbResults = articleDao.searchArticles(trimmed).first().map { it.toModel() }
                if (dbResults.isNotEmpty()) {
                    dbResults
                } else {
                    NinghsingCheContentData.articles.filter {
                        it.title.contains(trimmed, ignoreCase = true) ||
                                it.content.contains(trimmed, ignoreCase = true) ||
                                it.authorName.contains(trimmed, ignoreCase = true) ||
                                it.category.contains(trimmed, ignoreCase = true) ||
                                it.tags.any { tag -> tag.contains(trimmed, ignoreCase = true) }
                    }
                }
            }

            val filtered = all.filter { article ->
                val matchCategory = categorySlug == null || article.categorySlug == categorySlug
                val matchYear = year == null || article.year == year
                matchCategory && matchYear
            }

            emit(filtered)
        }
    }

    fun getCategories(): List<Category> = NinghsingCheContentData.categories

    fun getCategoryBySlug(slug: String): Category? = NinghsingCheContentData.categories.find { it.slug == slug }

    fun getAuthors(): List<Author> = NinghsingCheContentData.authors

    fun getAuthorById(id: String): Author? = NinghsingCheContentData.authors.find { it.id == id }

    fun getYearArchives(): List<YearArchive> = NinghsingCheContentData.yearArchives

    fun getYearArchiveByYear(year: Int): YearArchive? = NinghsingCheContentData.yearArchives.find { it.year == year }

    // PDF Publications & Magazines
    fun getPdfCategories(): List<PdfCategory> = NinghsingCheContentData.pdfCategories

    fun getPdfDocuments(): List<PdfDocument> = NinghsingCheContentData.pdfDocuments

    fun getPdfDocumentById(id: String): PdfDocument? = NinghsingCheContentData.pdfDocuments.find { it.id == id }

    fun getPdfDocumentsByCategory(categoryIdOrSlug: String): List<PdfDocument> {
        if (categoryIdOrSlug == "pdf-cat-all" || categoryIdOrSlug.isBlank()) return NinghsingCheContentData.pdfDocuments
        return NinghsingCheContentData.pdfDocuments.filter { it.categorySlug == categoryIdOrSlug || it.category == categoryIdOrSlug }
    }

    // Bookmarks
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

    // Reading History
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

    // Search History
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
