package com.example.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.data.portal.GalleryItem
import com.example.data.portal.PdfBook
import com.example.data.portal.VideoItem
import com.example.ui.editorial.ArticleRail
import com.example.ui.editorial.ArticleRow
import com.example.ui.editorial.AuthorRail
import com.example.ui.editorial.CategoryRail
import com.example.ui.editorial.EditorialSpace
import com.example.ui.editorial.EditorialType
import com.example.ui.editorial.EmptyState
import com.example.ui.editorial.ErrorState
import com.example.ui.editorial.GalleryGrid
import com.example.ui.editorial.Hairline
import com.example.ui.editorial.HeroArticleCard
import com.example.ui.editorial.LoadingFeed
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.editorial.NumberedArticleCard
import com.example.ui.editorial.PdfRail
import com.example.ui.editorial.SectionHeader
import com.example.ui.editorial.VideoRail

/**
 * Home — the magazine's front page.
 *
 * Structure, top to bottom: masthead → hero carousel → category rail →
 * featured rail → numbered specials → gallery strip → PDF shelf → video rail →
 * the running "latest" list. Each section is optional and disappears when the
 * API returns nothing for it, so the screen never shows an empty header.
 *
 * @param onArticleClick  receives the article id (used as the navigation key)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (CategoryRef) -> Unit,
    onAuthorClick: (AuthorRef) -> Unit,
    onSearchClick: () -> Unit,
    onGalleryClick: (GalleryItem) -> Unit,
    onPdfClick: (PdfBook) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllLatest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val offlineNotice by viewModel.offlineNotice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(offlineNotice) {
        offlineNotice?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = (state as? HomeUiState.Ready)?.feed?.settings?.title ?: "নিংশিং চে",
                        style = EditorialType.Masthead,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "অনুসন্ধান")
                    }
                    IconButton(onClick = { viewModel.load(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when (val current = state) {
            HomeUiState.Loading -> LoadingFeed(modifier = Modifier.padding(padding))

            is HomeUiState.Error -> ErrorState(
                message = current.message,
                onRetry = { viewModel.load(force = true) },
                modifier = Modifier.padding(padding)
            )

            is HomeUiState.Ready -> HomeContent(
                feed = current.feed,
                padding = padding,
                onArticleClick = onArticleClick,
                onCategoryClick = onCategoryClick,
                onAuthorClick = onAuthorClick,
                onGalleryClick = onGalleryClick,
                onPdfClick = onPdfClick,
                onVideoClick = onVideoClick,
                onSeeAllLatest = onSeeAllLatest
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    feed: com.example.data.portal.HomeFeed,
    padding: PaddingValues,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (CategoryRef) -> Unit,
    onAuthorClick: (AuthorRef) -> Unit,
    onGalleryClick: (GalleryItem) -> Unit,
    onPdfClick: (PdfBook) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllLatest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = EditorialSpace.xxl)
    ) {
        if (feed.settings.heroSliderEnabled && feed.hero.isNotEmpty()) {
            item { HeroCarousel(hero = feed.hero, onArticleClick = onArticleClick) }
        }

        if (feed.categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(EditorialSpace.sm))
                CategoryRail(
                    categories = feed.categories,
                    selectedSlug = null,
                    onSelect = onCategoryClick
                )
                Spacer(Modifier.height(EditorialSpace.sm))
                Hairline()
            }
        }

        if (feed.settings.featuredEnabled && feed.featured.isNotEmpty()) {
            item {
                ArticleRail(
                    title = "ফিচার্ড",
                    articles = feed.featured,
                    onArticleClick = { onArticleClick(it.id) },
                    onSeeAll = onSeeAllLatest
                )
            }
        }

        if (feed.special.isNotEmpty() && feed.settings.specialEnabled) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "বিশেষ নির্বাচন", subtitle = "সম্পাদকের পছন্দ")
                    feed.special.forEachIndexed { index, article ->
                        NumberedArticleCard(
                            index = index + 1,
                            article = article,
                            onClick = { onArticleClick(article.id) }
                        )
                        if (index < feed.special.lastIndex) {
                            Hairline(
                                modifier = Modifier.padding(
                                    horizontal = EditorialSpace.gutter,
                                    vertical = EditorialSpace.xxs
                                )
                            )
                        }
                    }
                }
            }
        }

        item { GalleryGrid(items = feed.gallery, onItemClick = onGalleryClick) }
        item { PdfRail(books = feed.pdfBooks, onBookClick = onPdfClick) }
        item { VideoRail(videos = feed.videos, onVideoClick = onVideoClick) }
        item { AuthorRail(authors = feed.authors, onAuthorClick = onAuthorClick) }

        if (feed.latest.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "সাম্প্রতিক",
                    subtitle = feed.settings.description,
                    actionLabel = "সব",
                    onAction = onSeeAllLatest
                )
            }
            items(feed.latest, key = { it.id }) { article ->
                ArticleRow(article = article, onClick = { onArticleClick(article.id) })
                Hairline(modifier = Modifier.padding(horizontal = EditorialSpace.gutter))
            }
        } else {
            item {
                EmptyState(message = "এখনো কোনো প্রবন্ধ প্রকাশিত হয়নি।")
            }
        }

        item { Footer(title = feed.settings.title) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    hero: List<ArticleSummary>,
    onArticleClick: (String) -> Unit
) {
    if (hero.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { hero.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            pageSpacing = EditorialSpace.md,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            HeroArticleCard(
                article = hero[page],
                onClick = { onArticleClick(hero[page].id) }
            )
        }
        if (hero.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EditorialSpace.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(hero.size) { index ->
                    val selected = pagerState.currentPage == index
                    val tokens = LocalEditorialTokens.current
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) tokens.accent else tokens.ruleStrong)
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Hairline()
        Spacer(Modifier.height(EditorialSpace.md))
        Text(
            text = title,
            style = EditorialType.Title,
            color = LocalEditorialTokens.current.inkSoft
        )
        Spacer(Modifier.height(EditorialSpace.xxs))
        Text(
            text = "ningshingche.com",
            style = EditorialType.Caption,
            color = LocalEditorialTokens.current.inkMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SeeAllButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EditorialSpace.md, vertical = EditorialSpace.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("সব", style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = LocalEditorialTokens.current.accent
            )
        }
    }
}
