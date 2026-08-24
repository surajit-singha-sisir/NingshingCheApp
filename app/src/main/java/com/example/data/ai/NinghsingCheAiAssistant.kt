package com.example.data.ai

import com.example.data.model.AiChatMessage
import com.example.data.model.Article
import com.example.data.model.ArticleCitation
import com.example.data.repository.ArticleRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class NinghsingCheAiAssistant(private val repository: ArticleRepository) {

    suspend fun answerQuestion(userQuestion: String): AiChatMessage {
        val query = userQuestion.trim()
        val articles = repository.getAllArticles().first()
        val live = articles.filter { !it.id.startsWith("art-") }
        val pool = if (live.isNotEmpty()) live else articles
        val sync = repository.syncState.value

        val tokens = tokenize(query)
        val ranked = pool.map { article ->
            article to score(article, tokens, query)
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6)

        val citations = ranked.map { (article, _) ->
            ArticleCitation(
                articleId = article.id,
                title = article.title,
                author = article.authorName,
                category = article.category,
                snippet = article.excerpt.ifBlank { article.content.take(140) }
            )
        }

        val answer = if (ranked.isEmpty()) {
            buildFallback(query, pool, sync.liveArticleCount, sync.usingLiveSite)
        } else {
            buildAnswer(query, ranked.map { it.first }, pool.size, sync.usingLiveSite)
        }

        return AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = answer,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations
        )
    }

    private fun tokenize(query: String): List<String> {
        return query.lowercase()
            .split(Regex("""[\s,।.?!:;“”"'()\[\]{}]+"""))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun score(article: Article, tokens: List<String>, raw: String): Int {
        val title = article.title
        val excerpt = article.excerpt
        val content = article.content
        val tags = article.tags.joinToString(" ")
        val meta = "${article.category} ${article.authorName} ${article.publishedDate}"
        var points = 0
        if (title.contains(raw, ignoreCase = true)) points += 12
        if (excerpt.contains(raw, ignoreCase = true)) points += 6
        tokens.forEach { token ->
            if (title.contains(token, ignoreCase = true)) points += 5
            if (tags.contains(token, ignoreCase = true)) points += 4
            if (excerpt.contains(token, ignoreCase = true)) points += 3
            if (meta.contains(token, ignoreCase = true)) points += 2
            if (content.contains(token, ignoreCase = true)) points += 1
        }
        return points
    }

    private fun buildAnswer(
        query: String,
        matches: List<Article>,
        archiveSize: Int,
        live: Boolean
    ): String {
        val source = if (live) "নিংশিংচে.কম থেকে সিঙ্ক করা লাইভ আর্কাইভ" else "অফলাইন আর্কাইভ"
        val top = matches.first()
        val snippet = top.content.ifBlank { top.excerpt }
            .replace(Regex("""\s+"""), " ")
            .take(420)
            .trim()
        val more = matches.drop(1).take(3).joinToString("\n") {
            "• ${it.title} — ${it.authorName} (${it.category})"
        }
        return buildString {
            append("আপনার প্রশ্ন: “$query”\n\n")
            append("$source-এর ${archiveSize}টি প্রবন্ধ খুঁজে সবচেয়ে মিল থাকা লেখা: ${top.title}।\n\n")
            if (snippet.isNotBlank()) {
                append(snippet)
                if (snippet.length >= 400) append("…")
                append("\n\n")
            }
            append("লেখক: ${top.authorName}  •  বিভাগ: ${top.category}  •  ${top.publishedDate}\n")
            if (more.isNotBlank()) {
                append("\nএই বিষয়ে আরও সিঙ্ক হওয়া প্রবন্ধ:\n")
                append(more)
            }
            append("\n\nনিচে তথ্যসূত্র থেকে মূল প্রবন্ধ খুলুন।")
        }
    }

    private fun buildFallback(
        query: String,
        pool: List<Article>,
        liveCount: Int,
        live: Boolean
    ): String {
        val cats = pool.groupingBy { it.category }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(6)
            .joinToString(" • ") { "${it.key} (${it.value})" }
        val source = if (live) "লাইভ সিঙ্ক" else "অফলাইন ক্যাশ"
        return """
            “$query” শব্দগুচ্ছ দিয়ে আর্কাইভে সরাসরি মিল পাওয়া যায়নি।

            এখন $source-এ ${if (liveCount > 0) liveCount else pool.size}টি প্রবন্ধ আছে।
            বিভাগ: $cats

            ইঞ্চৌঘর, মিংকৌ, ভাষা আন্দোলন, রাস, কবিতা বা কোনো লেখকের নাম দিয়ে আবার জিজ্ঞাসা করুন। হোম থেকে সিঙ্ক করলে নতুন লেখাও খোঁজা যাবে।
        """.trimIndent()
    }
}
