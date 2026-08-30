package com.example.ui.editorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.portal.ArticleSummary
import com.example.data.portal.AuthorRef
import com.example.data.portal.CategoryRef
import com.example.data.portal.GalleryItem
import com.example.data.portal.PdfBook
import com.example.data.portal.VideoItem
import com.example.data.portal.excerptOf
import com.example.data.portal.stripHtml
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reusable building blocks for the modern-editorial reader.
 *
 * Conventions
 * -----------
 * - Nothing here talks to a ViewModel or a repository; every composable takes
 *   plain data plus lambdas, so all of it renders in a preview.
 * - Images are always loaded through [EditorialImage], which owns the loading
 *   and error affordances. A missing image collapses to a tinted rule rather
 *   than an empty grey box.
 * - Cards are flat ([CardDefaults.cardElevation] stays at 0). Hierarchy comes
 *   from type scale, hairlines and whitespace.
 */

// ---------------------------------------------------------------------------
// Primitives
// ---------------------------------------------------------------------------

/** Full-width hairline, the editorial equivalent of a divider. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = LocalEditorialTokens.current.rule
) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalEditorialTokens.current.accent
) {
    Text(
        text = text,
        style = EditorialType.Eyebrow,
        color = color,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter)
            .padding(top = EditorialSpace.lg, bottom = EditorialSpace.sm),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = EditorialType.Headline,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = EditorialType.Caption,
                    color = LocalEditorialTokens.current.inkMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LocalEditorialTokens.current.accent
                )
            }
        }
    }
}

/** Coil image with a shimmering placeholder and a graceful error state. */
@Composable
fun EditorialImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(EditorialShape.thumb)
) {
    if (url.isBlank()) {
        EmptyThumb(modifier = modifier, shape = shape)
        return
    }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        error = null
    )
    // NOTE: coil's `error` slot is intentionally unused so a failed load keeps
    // the reserved space; wrap with EmptyThumb at the call site when a URL is
    // known to be missing.
}

@Composable
private fun EmptyThumb(modifier: Modifier = Modifier, shape: Shape) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(LocalEditorialTokens.current.surfaceSunken)
            .border(1.dp, LocalEditorialTokens.current.rule, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = LocalEditorialTokens.current.inkMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-progress"
    )
    val base = LocalEditorialTokens.current.surfaceSunken
    val highlight = base.copy(alpha = 0.55f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(0f, 0f),
        end = Offset(1000f * progress.coerceAtLeast(0.001f), 1000f * progress.coerceAtLeast(0.001f))
    )
}

// ---------------------------------------------------------------------------
// Meta / byline
// ---------------------------------------------------------------------------

/**
 * Formats `2025-06-17` as `১৭ জুন, ২০২৫`.
 *
 * Uses `SimpleDateFormat` rather than `java.time` on purpose: `minSdk` is 24 and
 * this module does not enable core library desugaring, so `java.time.*` would
 * throw `NoClassDefFoundError` on Android 7.
 */
fun formatBengaliDate(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10))
            ?: return iso.take(10)
        SimpleDateFormat("d MMMM, yyyy", Locale("bn", "BD")).format(parsed)
    }.getOrDefault(iso.take(10))
}

@Composable
fun Byline(
    authorName: String,
    publishedDate: String,
    readingTimeMinutes: Int,
    modifier: Modifier = Modifier,
    category: String? = null
) {
    val parts = buildList {
        if (authorName.isNotBlank()) add(authorName)
        if (!category.isNullOrBlank()) add(category)
        val date = formatBengaliDate(publishedDate)
        if (date.isNotBlank()) add(date)
        if (readingTimeMinutes > 0) add("$readingTimeMinutes মিনিট")
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString("  ·  "),
        style = EditorialType.Caption,
        color = LocalEditorialTokens.current.inkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun CategoryPill(
    title: String,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val tokens = LocalEditorialTokens.current
    val background = if (selected) tokens.accent else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else tokens.inkSoft
    val border = if (selected) Color.Transparent else tokens.rule
    Surface(
        shape = RoundedCornerShape(EditorialShape.chip),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = title,
            style = EditorialType.Caption,
            color = content,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Article cards
// ---------------------------------------------------------------------------

/** Full-bleed hero panel with a scrim and bottom-aligned headline. */
@Composable
fun HeroArticleCard(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(EditorialShape.card),
        colors = CardDefaults.cardColors(containerColor = LocalEditorialTokens.current.surfaceSunken),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x001A1512), Color(0xCC1A1512)),
                            startY = 120f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(EditorialSpace.md)
            ) {
                if (article.categoryTitle.isNotBlank()) {
                    Text(
                        text = article.categoryTitle,
                        style = EditorialType.Eyebrow,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = article.title,
                    style = EditorialType.Headline,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Byline(
                    authorName = article.authorName,
                    publishedDate = article.publishedDate,
                    readingTimeMinutes = article.readingTimeMinutes,
                    category = null
                )
            }
        }
    }
}

/** Landscape card used in horizontal rails: thumbnail left, text right. */
@Composable
fun RailArticleCard(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(EditorialShape.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.width(268.dp)
    ) {
        Column {
            EditorialImage(
                url = article.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(EditorialShape.card))
            )
            Column(Modifier.padding(EditorialSpace.md)) {
                Text(
                    text = article.title,
                    style = EditorialType.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Byline(
                    authorName = article.authorName,
                    publishedDate = article.publishedDate,
                    readingTimeMinutes = article.readingTimeMinutes
                )
            }
        }
    }
}

/** The workhorse list row: text on the left, thumbnail on the right. */
@Composable
fun ArticleRow(
    article: ArticleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (article.categoryTitle.isNotBlank()) {
                Eyebrow(article.categoryTitle)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = article.title,
                style = EditorialType.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (article.subTitle.isNotBlank()) {
                Text(
                    text = article.subTitle,
                    style = EditorialType.BodySmall,
                    color = LocalEditorialTokens.current.inkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Byline(
                authorName = article.authorName,
                publishedDate = article.publishedDate,
                readingTimeMinutes = article.readingTimeMinutes
            )
        }
        Spacer(Modifier.width(EditorialSpace.md))
        EditorialImage(
            url = article.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 96.dp, height = 72.dp)
        )
    }
}

/** Numbered card for "special articles" — the numeral is the accent. */
@Composable
fun NumberedArticleCard(
    index: Int,
    article: ArticleSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = String.format(Locale("bn", "BD"), "%d", index),
            style = EditorialType.Display,
            color = LocalEditorialTokens.current.accent.copy(alpha = 0.35f),
            modifier = Modifier.width(46.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = EditorialType.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Byline(
                authorName = article.authorName,
                publishedDate = article.publishedDate,
                readingTimeMinutes = article.readingTimeMinutes
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Rails
// ---------------------------------------------------------------------------

@Composable
fun CategoryRail(
    categories: List<CategoryRef>,
    selectedSlug: String?,
    onSelect: (CategoryRef) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
        horizontalArrangement = Arrangement.spacedBy(EditorialSpace.xs)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryPill(
                title = category.title,
                selected = category.slug == selectedSlug,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
fun ArticleRail(
    title: String,
    articles: List<ArticleSummary>,
    onArticleClick: (ArticleSummary) -> Unit,
    onSeeAll: (() -> Unit)? = null
) {
    if (articles.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            actionLabel = if (onSeeAll != null) "সব" else null,
            onAction = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(articles, key = { it.id }) { article ->
                RailArticleCard(article = article, onClick = { onArticleClick(article) })
            }
        }
    }
}

@Composable
fun AuthorRail(
    authors: List<AuthorRef>,
    onAuthorClick: (AuthorRef) -> Unit
) {
    if (authors.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "লেখক", subtitle = "নিংশিং চে-এর নিয়মিত কলম")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(authors, key = { it.id }) { author ->
                AuthorChip(author = author, onClick = { onAuthorClick(author) })
            }
        }
    }
}

@Composable
fun AuthorChip(author: AuthorRef, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(EditorialShape.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(168.dp)
    ) {
        Column(
            modifier = Modifier.padding(EditorialSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                EditorialImage(
                    url = author.imageUrl,
                    contentDescription = author.name,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                )
            }
            Spacer(Modifier.height(EditorialSpace.sm))
            Text(
                text = author.name,
                style = EditorialType.Subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (author.designation.isNotBlank()) {
                Text(
                    text = author.designation,
                    style = EditorialType.Caption,
                    color = LocalEditorialTokens.current.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GalleryGrid(
    items: List<GalleryItem>,
    onItemClick: (GalleryItem) -> Unit
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "ছবি ঘর", subtitle = "ইতিহাস ও সংস্কৃতির দৃশ্যপট")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.sm)
        ) {
            items(items, key = { it.id }) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = LocalEditorialTokens.current.surfaceSunken),
                    modifier = Modifier.width(180.dp)
                ) {
                    Box {
                        EditorialImage(
                            url = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC1A1512))))
                                .padding(EditorialSpace.sm)
                        ) {
                            Text(
                                text = item.title,
                                style = EditorialType.Caption,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfRail(
    books: List<PdfBook>,
    onBookClick: (PdfBook) -> Unit
) {
    if (books.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "বই ও সাময়িকী", subtitle = "ডাউনলোড করে পড়ুন")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(books, key = { it.id }) { book ->
                Card(
                    onClick = { onBookClick(book) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.width(148.dp)
                ) {
                    Column {
                        EditorialImage(
                            url = book.coverUrl,
                            contentDescription = book.title,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(EditorialSpace.sm)
                        )
                        Column(Modifier.padding(horizontal = EditorialSpace.md).padding(bottom = EditorialSpace.md)) {
                            Text(
                                text = book.title,
                                style = EditorialType.Subtitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (book.edition.isNotBlank()) {
                                Text(
                                    text = book.edition,
                                    style = EditorialType.Caption,
                                    color = LocalEditorialTokens.current.inkMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoRail(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "ভিডিও", subtitle = "নড়াচড়া ও কণ্ঠে সংস্কৃতি")
        LazyRow(
            contentPadding = PaddingValues(horizontal = EditorialSpace.gutter),
            horizontalArrangement = Arrangement.spacedBy(EditorialSpace.md)
        ) {
            items(videos, key = { it.id }) { video ->
                Card(
                    onClick = { onVideoClick(video) },
                    shape = RoundedCornerShape(EditorialShape.card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.width(252.dp)
                ) {
                    Column {
                        Box {
                            EditorialImage(
                                url = video.thumbnailUrl,
                                contentDescription = video.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                            Surface(
                                shape = RoundedCornerShape(EditorialShape.chip),
                                color = Color(0xCC1A1512),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(EditorialSpace.xs)
                            ) {
                                Text(
                                    text = video.platform,
                                    style = EditorialType.Caption,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = video.title,
                            style = EditorialType.Subtitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(EditorialSpace.md)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

@Composable
fun LoadingFeed(modifier: Modifier = Modifier, rows: Int = 5) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth().padding(EditorialSpace.gutter)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(EditorialShape.card))
                .background(brush)
        )
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = EditorialSpace.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
                Spacer(Modifier.width(EditorialSpace.md))
                Box(Modifier.size(width = 96.dp, height = 72.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.BrokenImage,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.xl, vertical = EditorialSpace.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LocalEditorialTokens.current.inkMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(EditorialSpace.md))
        Text(
            text = message,
            style = EditorialType.Body,
            color = LocalEditorialTokens.current.inkSoft,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(EditorialSpace.md))
            TextButton(onClick = onAction) {
                Text(actionLabel, style = EditorialType.Subtitle, color = LocalEditorialTokens.current.accent)
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        message = message,
        modifier = modifier,
        actionLabel = "আবার চেষ্টা করুন",
        onAction = onRetry
    )
}

/** Convenience used by list screens: a plain-text teaser from stored HTML. */
fun teaserOf(html: String) = excerptOf(stripHtml(html), 140)
