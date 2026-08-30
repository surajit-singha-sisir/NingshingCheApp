package com.example.ui.reader

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.example.data.portal.ArticleDetail
import com.example.data.portal.ArticleSummary
import com.example.data.portal.CommentItem
import com.example.data.portal.permalinkOf
import com.example.ui.editorial.ArticleRow
import com.example.ui.editorial.Byline
import com.example.ui.editorial.EditorialShape
import com.example.ui.editorial.EditorialSpace
import com.example.ui.editorial.EditorialType
import com.example.ui.editorial.EmptyState
import com.example.ui.editorial.ErrorState
import com.example.ui.editorial.Eyebrow
import com.example.ui.editorial.Hairline
import com.example.ui.editorial.LoadingFeed
import com.example.ui.editorial.LocalEditorialTokens
import com.example.ui.editorial.SectionHeader
import com.example.ui.editorial.formatBengaliDate

/**
 * The article reader.
 *
 * Layout: full-bleed hero → category eyebrow → serif headline → byline →
 * article body → tags → related media buttons → comments → related reading.
 *
 * The body is stored as HTML by the dashboard's Quill editor, so it is rendered
 * with a platform [TextView] through [HtmlCompat]. Compose has no HTML renderer,
 * and `HtmlCompat` already handles the tags the editor emits (`p`, `b`, `i`,
 * `a`, `br`, lists). It ignores inline `style` attributes such as
 * `text-align: center`, which is a known cosmetic limitation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    viewModel: ArticleViewModel,
    onBackClick: () -> Unit,
    onRelatedClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val article = (state as? ArticleUiState.Ready)?.article ?: return@IconButton
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${article.title}\n${permalinkOf(article.summary.slug)}")
                            putExtra(Intent.EXTRA_SUBJECT, article.title)
                        }
                        context.startActivity(Intent.createChooser(send, "শেয়ার করুন"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when (val current = state) {
            ArticleUiState.Loading -> LoadingFeed(modifier = Modifier.padding(padding))

            is ArticleUiState.Error -> ErrorState(
                message = current.message,
                onRetry = onBackClick,
                modifier = Modifier.padding(padding)
            )

            is ArticleUiState.Ready -> ArticleContent(
                article = current.article,
                comments = current.comments,
                related = current.related,
                padding = padding,
                onRelatedClick = onRelatedClick,
                onOpenLink = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onPostComment = { name, email, body ->
                    viewModel.postComment(name, email, body)
                },
                commentStatus = viewModel.commentStatus.collectAsState().value,
                isPostingComment = viewModel.isPostingComment.collectAsState().value
            )
        }
    }
}

@Composable
private fun ArticleContent(
    article: ArticleDetail,
    comments: List<CommentItem>,
    related: List<ArticleSummary>,
    padding: PaddingValues,
    onRelatedClick: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onPostComment: (String, String, String) -> Unit,
    commentStatus: String?,
    isPostingComment: Boolean
) {
    val tokens = LocalEditorialTokens.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = EditorialSpace.xxl)
    ) {
        item {
            if (article.summary.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = article.summary.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md)) {
                Eyebrow(article.summary.categoryTitle)
                Spacer(Modifier.height(EditorialSpace.xs))
                Text(
                    text = article.title,
                    style = EditorialType.Display,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (article.summary.subTitle.isNotBlank()) {
                    Spacer(Modifier.height(EditorialSpace.xs))
                    Text(
                        text = article.summary.subTitle,
                        style = EditorialType.Lede,
                        color = tokens.inkSoft
                    )
                }
                Spacer(Modifier.height(EditorialSpace.md))
                Byline(
                    authorName = article.summary.authorName,
                    publishedDate = article.summary.publishedDate,
                    readingTimeMinutes = article.summary.readingTimeMinutes
                )
                Spacer(Modifier.height(EditorialSpace.md))
                Hairline()
            }
        }

        item {
            HtmlArticleBody(
                html = article.html,
                modifier = Modifier.padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md)
            )
        }

        if (article.summary.tags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm),
                    horizontalArrangement = Arrangement.spacedBy(EditorialSpace.xs)
                ) {
                    article.summary.tags.take(6).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(EditorialShape.chip),
                            color = tokens.surfaceSunken
                        ) {
                            Text(
                                text = tag,
                                style = EditorialType.Caption,
                                color = tokens.inkSoft,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (article.videoLink.isNotBlank() || article.pdfLink.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm),
                    horizontalArrangement = Arrangement.spacedBy(EditorialSpace.sm)
                ) {
                    if (article.videoLink.isNotBlank()) {
                        FilledTonalButton(onClick = { onOpenLink(article.videoLink) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ভিডিও", style = EditorialType.Subtitle)
                        }
                    }
                    if (article.pdfLink.isNotBlank()) {
                        FilledTonalButton(onClick = { onOpenLink(article.pdfLink) }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("পিডিএফ", style = EditorialType.Subtitle)
                        }
                    }
                }
            }
        }

        item {
            Hairline(modifier = Modifier.padding(vertical = EditorialSpace.md))
            SectionHeader(
                title = "মন্তব্য",
                subtitle = if (comments.isEmpty()) "এখনো কোনো মন্তব্য নেই" else "${comments.size}টি মন্তব্য"
            )
        }

        if (comments.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = EditorialSpace.gutter)) {
                    Text(
                        text = "প্রথম মন্তব্যটি আপনিই লিখুন। অনুমোদনের পর তা এখানে প্রকাশিত হবে।",
                        style = EditorialType.BodySmall,
                        color = tokens.inkMuted
                    )
                }
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentRow(comment = comment)
            }
        }

        item {
            CommentForm(
                status = commentStatus,
                isPosting = isPostingComment,
                onSubmit = onPostComment
            )
        }

        if (related.isNotEmpty()) {
            item { SectionHeader(title = "এগুলোও পড়ুন") }
            items(related, key = { it.id }) { articleSummary ->
                ArticleRow(article = articleSummary, onClick = { onRelatedClick(articleSummary.id) })
                Hairline(modifier = Modifier.padding(horizontal = EditorialSpace.gutter))
            }
        }
    }
}

/**
 * Renders sanitised-for-display HTML in a [TextView].
 *
 * Sizing follows the reader's body scale so the article text matches the rest of
 * the screen, and the link colour is taken from the theme.
 */
@Composable
fun HtmlArticleBody(
    html: String,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 17f,
    lineSpacingMultiplier: Float = 1.65f
) {
    val tokens = LocalEditorialTokens.current
    if (html.isBlank()) {
        EmptyState(message = "এই প্রবন্ধের বিষয়বস্তু পাওয়া যায়নি।", modifier = modifier)
        return
    }
    val spanned: Spanned = remember(html) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                setLineSpacing(0f, lineSpacingMultiplier)
                setTextColor(android.graphics.Color.parseColor("#4A423A"))
                setLinkTextColor(android.graphics.Color.parseColor("#7A2E1E"))
            }
        },
        update = { textView ->
            textView.text = spanned
            textView.setTextColor(if (tokens.isDark) Color.LTGRAY else Color.DKGRAY)
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CommentRow(comment: CommentItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Comment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = LocalEditorialTokens.current.inkMuted
            )
            Spacer(Modifier.width(EditorialSpace.xs))
            Text(
                text = comment.name,
                style = EditorialType.Subtitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = comment.content,
            style = EditorialType.BodySmall,
            color = LocalEditorialTokens.current.inkSoft
        )
        Spacer(Modifier.height(6.dp))
        if (comment.createdAt.isNotBlank()) {
            Text(
                text = formatBengaliDate(comment.createdAt),
                style = EditorialType.Caption,
                color = LocalEditorialTokens.current.inkMuted
            )
        }
        Spacer(Modifier.height(EditorialSpace.sm))
        Hairline()
    }
}

@Composable
private fun CommentForm(
    status: String?,
    isPosting: Boolean,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var body by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EditorialSpace.gutter, vertical = EditorialSpace.md)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("নাম", style = EditorialType.BodySmall) },
            singleLine = true,
            textStyle = EditorialType.Body,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(EditorialSpace.sm))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল (ঐচ্ছিক)", style = EditorialType.BodySmall) },
            singleLine = true,
            textStyle = EditorialType.Body,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(EditorialSpace.sm))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("মন্তব্য", style = EditorialType.BodySmall) },
            minLines = 3,
            textStyle = EditorialType.Body,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(EditorialSpace.md))
        Button(
            onClick = { onSubmit(name.text, email.text, body.text) },
            enabled = !isPosting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(EditorialSpace.xs))
            }
            Text("মন্তব্য পাঠান", style = EditorialType.Subtitle)
        }
        if (!status.isNullOrBlank()) {
            Spacer(Modifier.height(EditorialSpace.sm))
            Text(
                text = status,
                style = EditorialType.Caption,
                color = LocalEditorialTokens.current.accent
            )
        }
    }
}

@Composable
internal fun ArticleThumb(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(EditorialShape.thumb))
    )
}

@Composable
internal fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = EditorialType.Body, color = LocalEditorialTokens.current.inkMuted)
    }
}
