package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ReaderThemeMode
import com.example.ui.components.ArticleListItemCard
import com.example.ui.components.ArticleReaderSkeleton
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.CrispCanvas
import com.example.ui.theme.CrispText
import com.example.ui.theme.NightCanvas
import com.example.ui.theme.NightTextPrimary
import com.example.ui.theme.PaperCanvasLight
import com.example.ui.theme.SepiaCanvas
import com.example.ui.theme.SepiaText
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    articleId: String,
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onRelatedArticleClick: (String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    val article by viewModel.currentArticle.collectAsStateWithLifecycle()
    val relatedArticles by viewModel.relatedArticles.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val ttsStatusText by viewModel.ttsProgressText.collectAsStateWithLifecycle()

    var showAppearanceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val listState = rememberLazyListState()

    // Determine reader background and text colors based on selected reading theme
    val (readerBgColor, readerTextColor) = when (preferences.themeMode) {
        ReaderThemeMode.PAPER -> PaperCanvasLight to TextPrimaryLight
        ReaderThemeMode.SEPIA -> SepiaCanvas to SepiaText
        ReaderThemeMode.NIGHT -> NightCanvas to NightTextPrimary
        ReaderThemeMode.CRISP -> CrispCanvas to CrispText
    }

    // Dynamic high-contrast link and accent color configured for both Dark and Light modes
    val linkColor = when (preferences.themeMode) {
        ReaderThemeMode.NIGHT -> Color(0xFFFBBF24) // Bright Gold/Amber for dark theme
        ReaderThemeMode.SEPIA -> Color(0xFF92400E) // Warm Amber-Brown
        ReaderThemeMode.CRISP -> Color(0xFF0284C7) // Vibrant Sky Blue
        ReaderThemeMode.PAPER -> Color(0xFFB45309) // Editorial Amber
    }

    val linkContainerColor = when (preferences.themeMode) {
        ReaderThemeMode.NIGHT -> Color(0xFF1E293B)
        ReaderThemeMode.SEPIA -> Color(0xFFEADBCE)
        ReaderThemeMode.CRISP -> Color(0xFFF0F9FF)
        ReaderThemeMode.PAPER -> Color(0xFFFEF3C7)
    }

    // Save reading position
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        if (article != null) {
            val progress = (firstVisibleIndex.toFloat() / 5f).coerceIn(0f, 1f)
            viewModel.updateReadingProgress(firstVisibleIndex, progress)
        }
    }

    var isSkeletonLoading by remember(articleId) { mutableStateOf(true) }
    LaunchedEffect(articleId) {
        kotlinx.coroutines.delay(1000L) // Minimum 1 second skeleton view
        isSkeletonLoading = false
    }

    if (article == null || isSkeletonLoading) {
        ArticleReaderSkeleton()
        return
    }

    val currentArt = article!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Reader Top Navigation Bar
            Surface(
                color = readerBgColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = readerTextColor
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Appearance / Typography Sheet Button
                        IconButton(
                            onClick = { showAppearanceSheet = true },
                            modifier = Modifier.testTag("reader_font_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Font Settings",
                                tint = readerTextColor
                            )
                        }

                        // Audio Text-To-Speech Button
                        IconButton(
                            onClick = { viewModel.toggleTts() },
                            modifier = Modifier.testTag("reader_tts_button")
                        ) {
                            Icon(
                                imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                tint = if (isTtsPlaying) MaterialTheme.colorScheme.primary else readerTextColor
                            )
                        }

                        // Bookmark Button
                        IconButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier.testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else readerTextColor
                            )
                        }

                        // Share Button
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, currentArt.title)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${currentArt.title}\n\n${currentArt.excerpt}\n\nনিংশিং চে ডিজিটাল তথ্যকোষে সম্পূর্ণ প্রবন্ধটি পড়ুন:\n${currentArt.sourceUrl}"
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "প্রবন্ধটি শেয়ার করুন")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.testTag("reader_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = readerTextColor
                            )
                        }
                    }
                }
            }

            // Text-To-Speech Floating Active Bar
            if (isTtsPlaying || ttsStatusText.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = ttsStatusText.ifEmpty { "অডিও পাঠ চলছে..." },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleTts() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.stopTts() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Article Content Reader Scroll
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("article_reader_content"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category & Date metadata header with Google Icons
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = linkContainerColor
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(currentArt.categorySlug),
                                    contentDescription = null,
                                    tint = linkColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = currentArt.category,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = linkColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = readerTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = currentArt.publishedDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = readerTextColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = readerTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${currentArt.readingTimeMinutes} মিনিট পাঠ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = readerTextColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Article Headline
                item {
                    Text(
                        text = currentArt.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = readerTextColor,
                            fontSize = (preferences.fontSizeSp + 7).sp,
                            lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier + 10).sp
                        )
                    )
                }

                // Author Row with Google Icons
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = linkContainerColor.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, linkColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAuthorClick(currentArt.authorId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box {
                                AsyncImage(
                                    model = currentArt.authorAvatarUrl.ifEmpty { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&q=80" },
                                    contentDescription = currentArt.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentArt.authorName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = readerTextColor,
                                        fontSize = 15.sp
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = linkColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "গবেষক ও লেখক • নিংশিং চে ডিজিটাল আর্কাইভ",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = linkColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Author profile",
                                tint = linkColor,
                                modifier = Modifier
                                    .size(16.dp)
                            )
                        }
                    }
                }

                // Hero Image
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = currentArt.featuredImageUrl,
                            contentDescription = currentArt.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                        )
                    }
                }

                // Excerpt / Abstract Blockquote
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = linkContainerColor,
                        border = BorderStroke(1.dp, linkColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentArt.excerpt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                color = readerTextColor,
                                fontSize = (preferences.fontSizeSp - 1).sp,
                                lineHeight = (preferences.fontSizeSp * 1.5).sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Main Article Body Paragraphs
                val paragraphs = currentArt.content.split("\n\n")
                items(paragraphs) { paragraph ->
                    if (paragraph.isNotBlank()) {
                        Text(
                            text = paragraph.trim(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = readerTextColor,
                                fontSize = preferences.fontSizeSp.sp,
                                lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp,
                                letterSpacing = 0.3.sp
                            )
                        )
                    }
                }

                // Dark & Light Mode Configured Web Source & Citation Links Box
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = linkContainerColor,
                        border = BorderStroke(1.dp, linkColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentArt.sourceUrl))
                                context.startActivity(browserIntent)
                            }
                            .testTag("article_source_link")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = linkColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (preferences.themeMode == ReaderThemeMode.NIGHT) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "অনলাইন মূল উৎস ও রেফারেন্স লিংক",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = linkColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = currentArt.sourceUrl,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = readerTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "নিংশিং চে ডিজিটাল আর্কাইভে সরাসরি পড়ুন ›",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = linkColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Web Link",
                                tint = linkColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Tags with Google Icons
                if (currentArt.tags.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = linkColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "সম্পর্কিত বিষয় ও ট্যাগ:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = linkColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(currentArt.tags) { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = linkContainerColor,
                                        border = BorderStroke(1.dp, linkColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = linkColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider
                item {
                    HorizontalDivider(
                        color = linkColor.copy(alpha = 0.25f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // Related Articles
                if (relatedArticles.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = linkColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "এই বিষয়ের আরও নির্বাচিত প্রবন্ধ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = readerTextColor,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            relatedArticles.forEach { rel ->
                                ArticleListItemCard(
                                    article = rel,
                                    onClick = { onRelatedArticleClick(rel.id) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Appearance & Typography Modal Bottom Sheet (100% Bengali, NO English in parentheses)
        if (showAppearanceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAppearanceSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "পাঠের স্বাচ্ছন্দ্য ও ডিসপ্লে সেটিংস",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                        )
                    }

                    // Theme Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "পৃষ্ঠার ব্যাকগ্রাউন্ড থিম",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val themes = listOf(
                                Triple(ReaderThemeMode.PAPER, "কাগজ", PaperCanvasLight),
                                Triple(ReaderThemeMode.SEPIA, "সেপিয়া", SepiaCanvas),
                                Triple(ReaderThemeMode.NIGHT, "রাত্রি", NightCanvas),
                                Triple(ReaderThemeMode.CRISP, "শ্বেত", CrispCanvas)
                            )
                            themes.forEach { (mode, label, color) ->
                                val isSelected = preferences.themeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = color,
                                    border = BorderStroke(
                                        2.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { viewModel.updateThemeMode(mode) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (mode == ReaderThemeMode.NIGHT) NightTextPrimary else TextPrimaryLight
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Font Size Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "অক্ষরের আকার",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "${preferences.fontSizeSp.toInt()} sp",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Slider(
                            value = preferences.fontSizeSp,
                            onValueChange = { viewModel.updateFontSize(it) },
                            valueRange = 13f..24f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Line Spacing Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "লাইনের ফাঁক",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = String.format("%.1fx", preferences.lineSpacingMultiplier),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Slider(
                            value = preferences.lineSpacingMultiplier,
                            onValueChange = { viewModel.updateLineSpacing(it) },
                            valueRange = 1.3f..2.2f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
