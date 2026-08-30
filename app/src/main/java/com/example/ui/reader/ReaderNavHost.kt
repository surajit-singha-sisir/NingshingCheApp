package com.example.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.data.portal.PortalRepository
import java.net.URLEncoder

/**
 * Navigation graph for the public reader.
 *
 * Routes
 * ------
 * - `home`                     front page
 * - `article/{articleId}`      reader, keyed by UUID **or** slug
 * - `category/{categorySlug}`  paged list for one category
 * - `author/{authorId}`        paged list for one author
 * - `search`                   server-side search
 *
 * `article/{articleId}` also accepts the website permalinks
 * `https://ningshingche.com/article/{slug}` and
 * `https://ningshingche.com/{id}` so links shared from the site open in the app.
 *
 * Slugs in this database are Bengali, so they are percent-encoded when building
 * a route and decoded again at the destination.
 */
object ReaderRoute {
    const val Home = "home"
    const val Search = "search"
    const val Article = "article/{articleId}"
    const val Category = "category/{categorySlug}"
    const val Author = "author/{authorId}"

    fun article(idOrSlug: String) = "article/${encode(idOrSlug)}"
    fun category(slug: String) = "category/${encode(slug)}"
    fun author(id: String) = "author/${encode(id)}"

    private fun encode(value: String) =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

@Composable
fun EditorialReaderApp(
    repository: PortalRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factory = ReaderViewModelFactory(repository)

    val openExternal: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ReaderRoute.Home,
        modifier = modifier
    ) {
        composable(ReaderRoute.Home) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = homeViewModel,
                onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                onCategoryClick = { navController.navigate(ReaderRoute.category(it.slug)) },
                onAuthorClick = { navController.navigate(ReaderRoute.author(it.id)) },
                onSearchClick = { navController.navigate(ReaderRoute.Search) },
                onGalleryClick = { openExternal(it.imageUrl) },
                onPdfClick = { openExternal(it.fileUrl) },
                onVideoClick = { openExternal(it.url) },
                onSeeAllLatest = { navController.navigate(ReaderRoute.Search) }
            )
        }

        composable(ReaderRoute.Search) {
            val searchViewModel: SearchViewModel = viewModel(factory = factory)
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onArticleClick = { navController.navigate(ReaderRoute.article(it)) },
                onCategoryClick = { navController.navigate(ReaderRoute.category(it.slug)) }
            )
        }

        composable(
            route = ReaderRoute.Article,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://ningshingche.com/article/{articleId}" },
                navDeepLink { uriPattern = "http://ningshingche.com/article/{articleId}" },
                navDeepLink { uriPattern = "https://ningshingche.com/{articleId}" },
                navDeepLink { uriPattern = "http://ningshingche.com/{articleId}" }
            )
        ) { entry ->
            val articleId = entry.arguments?.getString("articleId").orEmpty()
            val articleViewModel: ArticleViewModel = viewModel(factory = factory)
            LaunchedEffect(articleId) { articleViewModel.load(articleId) }
            ArticleScreen(
                viewModel = articleViewModel,
                onBackClick = { navController.popBackStack() },
                onRelatedClick = { navController.navigate(ReaderRoute.article(it)) }
            )
        }

        composable(
            route = ReaderRoute.Category,
            arguments = listOf(navArgument("categorySlug") { type = NavType.StringType })
        ) { entry ->
            val slug = entry.arguments?.getString("categorySlug").orEmpty()
            val categoryViewModel: CategoryViewModel = viewModel(
                factory = CategoryViewModelFactory(repository, slug)
            )
            CategoryScreen(
                viewModel = categoryViewModel,
                onBackClick = { navController.popBackStack() },
                onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
            )
        }

        composable(
            route = ReaderRoute.Author,
            arguments = listOf(navArgument("authorId") { type = NavType.StringType })
        ) { entry ->
            val authorId = entry.arguments?.getString("authorId").orEmpty()
            val authorViewModel: AuthorViewModel = viewModel(
                factory = AuthorViewModelFactory(repository, authorId)
            )
            AuthorScreen(
                viewModel = authorViewModel,
                onBackClick = { navController.popBackStack() },
                onArticleClick = { navController.navigate(ReaderRoute.article(it)) }
            )
        }
    }
}
