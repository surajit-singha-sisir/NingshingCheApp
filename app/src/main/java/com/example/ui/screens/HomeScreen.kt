package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AuthorRailCard
import com.example.ui.components.CategoryImageTile
import com.example.ui.components.FeaturedPortalCard
import com.example.ui.components.HeroArticleCarousel
import com.example.ui.components.HomeSkeletonLayout
import com.example.ui.components.HorizontalCardsRow
import com.example.ui.components.PdfBookRailCard
import com.example.ui.components.PortalSectionHeader
import com.example.ui.components.SelectedEssayCard
import com.example.ui.components.SubmitWritingBanner
import com.example.ui.theme.Kalpurush
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMenuClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onArchiveClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onPdfArchiveClick: () -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onFeaturedClick: () -> Unit
) {
    val allArticles by viewModel.allArticles.collectAsStateWithLifecycle()
    val featuredArticles by viewModel.featuredArticles.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    val pdfDocuments by viewModel.pdfDocuments.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var isSkeletonLoading by remember { mutableStateOf(true) }
    LaunchedEffect(allArticles.size) {
        if (allArticles.isEmpty()) delay(900L)
        isSkeletonLoading = allArticles.isEmpty()
    }

    val heroArticles = remember(featuredArticles, allArticles) {
        val combined = (featuredArticles + allArticles).distinctBy { it.id }
        combined.take(3)
    }
    val featuredRail = remember(featuredArticles, allArticles) {
        val list = featuredArticles.ifEmpty { allArticles.filter { it.isFeatured } }
        list.distinctBy { it.id }.take(6)
    }
    val selectedEssays = remember(allArticles, featuredRail) {
        val featuredIds = featuredRail.map { it.id }.toSet()
        allArticles.filter { it.id !in featuredIds }.take(8).ifEmpty { allArticles.take(6) }
    }
    val categoryCovers = remember(categories, allArticles) {
        categories.associate { cat ->
            cat.slug to (
                cat.imageUrl.ifBlank {
                    allArticles.firstOrNull { it.categorySlug == cat.slug && it.featuredImageUrl.isNotBlank() }?.featuredImageUrl.orEmpty()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isSkeletonLoading) {
            HomeSkeletonLayout()
        } else {
            PullToRefreshBox(
                isRefreshing = syncState.isSyncing,
                onRefresh = { viewModel.refreshFromWebsite() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_screen_list"),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (syncState.isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        syncState.isSyncing -> "নিংশিংচে.কম থেকে হালনাগাদ হচ্ছে..."
                                        syncState.usingLiveSite -> "লাইভ আর্কাইভ • ${allArticles.size}টি প্রবন্ধ"
                                        else -> "অফলাইন আর্কাইভ • নিংশিংচে.কম সিঙ্ক করুন"
                                    },
                                    fontFamily = Kalpurush,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.refreshFromWebsite() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("home_sync_refresh")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (heroArticles.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                HeroArticleCarousel(articles = heroArticles, onArticleClick = onArticleClick)
                            }
                        }
                    }

                    if (featuredRail.isNotEmpty()) {
                        item { PortalSectionHeader("ফিচার্ড আর্টিকেল", "সব দেখুন ›", onFeaturedClick) }
                        item {
                            HorizontalCardsRow {
                                items(featuredRail, key = { it.id }) { article ->
                                    FeaturedPortalCard(
                                        article = article,
                                        onClick = { onArticleClick(article.id) },
                                        modifier = Modifier.fillParentMaxWidth(0.92f)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedEssays.isNotEmpty()) {
                        item { PortalSectionHeader("নির্বাচিত প্রবন্ধ") }
                        item {
                            HorizontalCardsRow {
                                items(selectedEssays, key = { it.id }) { article ->
                                    SelectedEssayCard(article) { onArticleClick(article.id) }
                                }
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        item { PortalSectionHeader("জনপ্রিয় বিষয় ও বিভাগ", "সব দেখুন ›", onSeeAllCategoriesClick) }
                        item {
                            HorizontalCardsRow {
                                items(categories, key = { it.slug }) { category ->
                                    CategoryImageTile(
                                        category = category,
                                        imageUrl = categoryCovers[category.slug].orEmpty(),
                                        onClick = { onCategoryClick(category.slug) }
                                    )
                                }
                            }
                        }
                    }

                    if (authors.isNotEmpty()) {
                        item { PortalSectionHeader("আমার লেখক পারেঙ", "সব দেখুন ›", onAuthorsClick) }
                        item {
                            HorizontalCardsRow {
                                items(authors.take(16), key = { it.id }) { author ->
                                    AuthorRailCard(author) { onAuthorClick(author.id) }
                                }
                            }
                        }
                    }

                    if (pdfDocuments.isNotEmpty()) {
                        item { PortalSectionHeader("পিডিএফ(PDF) লেরিক", "আর্কাইভ ›", onPdfArchiveClick) }
                        item {
                            HorizontalCardsRow {
                                items(pdfDocuments, key = { it.id }) { pdf ->
                                    PdfBookRailCard(pdf) { onPdfArchiveClick() }
                                }
                            }
                        }
                    }

                    item { SubmitWritingBanner(onClick = onSubmitClick) }
                }
            }
        }
    }
}
