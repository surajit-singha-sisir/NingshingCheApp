package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object Search : Screen("search")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object AiAssistant : Screen("ai_assistant")
    data object Settings : Screen("settings")
    data object Dashboard : Screen("dashboard")
    data object PdfArchive : Screen("pdf_archive")

    data object PdfViewer : Screen("pdf_viewer/{pdfId}") {
        fun createRoute(pdfId: String) = "pdf_viewer/$pdfId"
    }

    data object ArticleDetail : Screen("article/{articleId}") {
        fun createRoute(articleId: String) = "article/$articleId"
    }

    data object CategoryDetail : Screen("category/{categorySlug}") {
        fun createRoute(categorySlug: String) = "category/$categorySlug"
    }

    data object AuthorDetail : Screen("author/{authorId}") {
        fun createRoute(authorId: String) = "author/$authorId"
    }

    data object ArchiveDetail : Screen("archive/{year}") {
        fun createRoute(year: Int) = "archive/$year"
    }
}
