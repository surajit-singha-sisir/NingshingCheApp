package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Article
import com.example.data.model.Author
import com.example.ui.components.ArticleListItemCard
import com.example.ui.components.AuthorCardItem
import com.example.ui.theme.Kalpurush
import com.example.ui.viewmodel.ExploreViewModel
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun FeaturedScreen(
    viewModel: HomeViewModel,
    onArticleClick: (String) -> Unit
) {
    val articles by viewModel.allArticles.collectAsStateWithLifecycle()
    val featured = articles.filter { it.isFeatured || it.isEditorialPick }.ifEmpty { articles.take(12) }
    PortalListPage(
        title = "ফিচার্ড",
        subtitle = "নিংশিংচে.কম-এর নির্বাচিত প্রবন্ধ",
        articles = featured,
        onArticleClick = onArticleClick
    )
}

@Composable
fun AuthorsDirectoryScreen(
    viewModel: ExploreViewModel,
    onAuthorClick: (String) -> Unit
) {
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PageIntro("লেখক", "নিংশিং চে তথ্যকোষের লেখক ও গবেষকবৃন্দ")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(authors, key = { it.id }) { author: Author ->
                AuthorCardItem(author = author, onClick = { onAuthorClick(author.id) })
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val uri = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PageIntro("আমার সম্পর্কে", "নিংশিং চে — বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "নিংশিং চে বিষ্ণুপ্রিয়া মণিপুরি ভাষা, সাহিত্য, ইতিহাস ও সংস্কৃতির ডিজিটাল তথ্যকোষ। পোর্টালটি তিলকপুর, কমলগঞ্জ, মৌলভীবাজার, সিলেট থেকে পরিচালিত।",
                            fontFamily = Kalpurush,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("ঠিকানা: তিলকপুর, কমলগঞ্জ, মৌলভীবাজার, সিলেট", fontFamily = Kalpurush, fontSize = 15.sp)
                        Text("ফোন: +880 9638-781890", fontFamily = Kalpurush, fontSize = 15.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                "ওয়েবসাইট",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = Kalpurush,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        androidx.compose.material3.TextButton(onClick = { uri.openUri("https://ningshingche.com/about-us") }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Text("  ningshingche.com/about-us", fontFamily = Kalpurush)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialActivitiesScreen(
    viewModel: ExploreViewModel,
    onArticleClick: (String) -> Unit
) {
    val articles by viewModel.allArticles.collectAsStateWithLifecycle()
    val social = articles.filter {
        it.categorySlug.contains("social") ||
            it.category.contains("সামাজিক") ||
            it.tags.any { tag -> tag.contains("সামাজিক") }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PageIntro("সামাজিক কার্যকলাপ", "সমাজ, সংগঠন ও সাংস্কৃতিক উদ্যোগ")
        if (social.isEmpty()) {
            Text(
                "এই বিভাগের লাইভ লেখা সিঙ্ক হলে এখানে দেখাবে। ইতিমধ্যে সমাজ ও সংস্কৃতি বিভাগ ঘুরে দেখুন।",
                fontFamily = Kalpurush,
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(social.ifEmpty { articles.filter { it.categorySlug == "society-culture" } }) { article ->
                ArticleListItemCard(article, onClick = { onArticleClick(article.id) })
            }
        }
    }
}

@Composable
private fun PortalListPage(
    title: String,
    subtitle: String,
    articles: List<Article>,
    onArticleClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PageIntro(title, subtitle)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(articles, key = { it.id }) { article ->
                ArticleListItemCard(article, onClick = { onArticleClick(article.id) })
            }
        }
    }
}

@Composable
private fun PageIntro(title: String, subtitle: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, fontFamily = Kalpurush, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Text(subtitle, fontFamily = Kalpurush, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
