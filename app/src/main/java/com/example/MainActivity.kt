package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.data.model.AppThemeMode
import com.example.data.model.ReaderPreferences
import com.example.ui.components.EditorialBottomNavBar
import com.example.ui.components.EditorialNavigationDrawerContent
import com.example.ui.navigation.Screen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ArchiveYearDetailScreen
import com.example.ui.screens.ArticleReaderScreen
import com.example.ui.screens.AuthorDetailScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfArchiveScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.BookmarksViewModel
import com.example.ui.viewmodel.ExploreViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.PdfArchiveViewModel
import com.example.ui.viewmodel.PdfViewerViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NinghsingCheApp
        val factory = ViewModelFactory(
            repository = app.articleRepository,
            preferencesRepository = app.preferencesRepository,
            aiAssistant = app.aiAssistant,
            dashboardRepository = app.dashboardRepository,
            context = applicationContext
        )

        setContent {
            val preferences by app.preferencesRepository.readerPreferences
                .collectAsStateWithLifecycle(initialValue = ReaderPreferences())
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (preferences.appThemeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                NinghsingCheAppRoot(factory)
            }
        }
    }
}

@Composable
fun NinghsingCheAppRoot(factory: ViewModelFactory) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val exploreViewModel: ExploreViewModel = viewModel(factory = factory)
    val searchViewModel: SearchViewModel = viewModel(factory = factory)
    val readerViewModel: ReaderViewModel = viewModel(factory = factory)
    val bookmarksViewModel: BookmarksViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val aiViewModel: AiViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val pdfArchiveViewModel: PdfArchiveViewModel = viewModel(factory = factory)
    val pdfViewerViewModel: PdfViewerViewModel = viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)

    // Screens where bottom bar should be visible
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Bookmarks.route,
        Screen.PdfArchive.route
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            EditorialNavigationDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onVisitWebsite = {
                    scope.launch {
                        drawerState.close()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ningshingche.com"))
                        context.startActivity(intent)
                    }
                },
                onShareApp = {
                    scope.launch {
                        drawerState.close()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "নিংশিং চে - বিষ্ণুপ্রিয়া মণিপুরি ডিজিটাল সাংস্কৃতিক আর্কাইভ ও সাহিত্য পত্রিকা। ডাউনলোড করুন ও পড়ুন: https://ningshingche.com"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "নিংশিং চে শেয়ার করুন"))
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    EditorialBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(280)) + slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(280)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(240)) + slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(240)
                        )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(280)) + slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(280)
                        )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(240)) + slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(240)
                        )
                    }
                ) {
                    // Home Screen
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            },
                            onCategoryClick = { categorySlug ->
                                navController.navigate(Screen.CategoryDetail.createRoute(categorySlug))
                            },
                            onAuthorClick = { authorId ->
                                navController.navigate(Screen.AuthorDetail.createRoute(authorId))
                            },
                            onArchiveClick = { year ->
                                navController.navigate(Screen.ArchiveDetail.createRoute(year))
                            },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route)
                            },
                            onAiClick = {
                                navController.navigate(Screen.AiAssistant.route)
                            },
                            onPdfArchiveClick = {
                                navController.navigate(Screen.PdfArchive.route)
                            },
                            onSeeAllCategoriesClick = {
                                navController.navigate(Screen.Explore.route)
                            }
                        )
                    }

                    // Explore Screen
                    composable(Screen.Explore.route) {
                        ExploreScreen(
                            viewModel = exploreViewModel,
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            },
                            onCategoryClick = { categorySlug ->
                                navController.navigate(Screen.CategoryDetail.createRoute(categorySlug))
                            },
                            onAuthorClick = { authorId ->
                                navController.navigate(Screen.AuthorDetail.createRoute(authorId))
                            },
                            onArchiveClick = { year ->
                                navController.navigate(Screen.ArchiveDetail.createRoute(year))
                            }
                        )
                    }

                    // PDF Archive Screen
                    composable(Screen.PdfArchive.route) {
                        PdfArchiveScreen(
                            viewModel = pdfArchiveViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenPdf = { pdfId ->
                                navController.navigate(Screen.PdfViewer.createRoute(pdfId))
                            }
                        )
                    }

                    // PDF Viewer Screen
                    composable(
                        route = Screen.PdfViewer.route,
                        arguments = listOf(navArgument("pdfId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pdfId = backStackEntry.arguments?.getString("pdfId") ?: "pdf-2025"
                        PdfViewerScreen(
                            pdfId = pdfId,
                            viewModel = pdfViewerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Search Screen
                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // Bookmarks Screen
                    composable(Screen.Bookmarks.route) {
                        BookmarksScreen(
                            viewModel = bookmarksViewModel,
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // History Screen
                    composable(Screen.History.route) {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onBackClick = { navController.popBackStack() },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // AI Assistant Screen
                    composable(Screen.AiAssistant.route) {
                        AiAssistantScreen(
                            viewModel = aiViewModel,
                            onBackClick = { navController.popBackStack() },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // Settings Screen
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // Dashboard CMS Control Center
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToReaderView = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Article Reader Screen with Deep Links
                    composable(
                        route = "web_article/{year}/{month}/{slug}",
                        arguments = listOf(
                            navArgument("year") { type = NavType.StringType },
                            navArgument("month") { type = NavType.StringType },
                            navArgument("slug") { type = NavType.StringType }
                        ),
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "https://ningshingche.com/{year}/{month}/{slug}" },
                            navDeepLink { uriPattern = "http://ningshingche.com/{year}/{month}/{slug}" }
                        )
                    ) { backStackEntry ->
                        val year = backStackEntry.arguments?.getString("year").orEmpty()
                        val month = backStackEntry.arguments?.getString("month").orEmpty()
                        val slug = backStackEntry.arguments?.getString("slug").orEmpty()
                            .removeSuffix(".kehem")
                        val articleId = if (year.isNotBlank() && month.isNotBlank()) {
                            "$year/$month/$slug"
                        } else {
                            slug.ifBlank { "art-1" }
                        }
                        ArticleReaderScreen(
                            articleId = articleId,
                            viewModel = readerViewModel,
                            onBackClick = { navController.popBackStack() },
                            onAuthorClick = { authorId ->
                                navController.navigate(Screen.AuthorDetail.createRoute(authorId))
                            },
                            onRelatedArticleClick = { nextId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(nextId))
                            }
                        )
                    }

                    composable(
                        route = Screen.ArticleDetail.route,
                        arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "https://ningshingche.com/{articleId}" },
                            navDeepLink { uriPattern = "http://ningshingche.com/{articleId}" },
                            navDeepLink { uriPattern = "https://ningshingche.com/article/{articleId}" },
                            navDeepLink { uriPattern = "http://ningshingche.com/article/{articleId}" }
                        )
                    ) { backStackEntry ->
                        val articleId = backStackEntry.arguments?.getString("articleId") ?: "art-1"
                        ArticleReaderScreen(
                            articleId = articleId,
                            viewModel = readerViewModel,
                            onBackClick = { navController.popBackStack() },
                            onAuthorClick = { authorId ->
                                navController.navigate(Screen.AuthorDetail.createRoute(authorId))
                            },
                            onRelatedArticleClick = { nextId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(nextId))
                            }
                        )
                    }

                    // Category Detail Screen
                    composable(
                        route = Screen.CategoryDetail.route,
                        arguments = listOf(navArgument("categorySlug") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val slug = backStackEntry.arguments?.getString("categorySlug") ?: ""
                        val allArticles by exploreViewModel.allArticles.collectAsStateWithLifecycle()
                        val liveCategories by exploreViewModel.categories.collectAsStateWithLifecycle()
                        val category = liveCategories.find { it.slug == slug }
                        val filteredArticles = allArticles.filter { it.categorySlug == slug }

                        CategoryDetailScreen(
                            category = category,
                            articles = filteredArticles,
                            onBackClick = { navController.popBackStack() },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // Author Detail Screen
                    composable(
                        route = Screen.AuthorDetail.route,
                        arguments = listOf(navArgument("authorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
                        val allArticles by exploreViewModel.allArticles.collectAsStateWithLifecycle()
                        val liveAuthors by exploreViewModel.authors.collectAsStateWithLifecycle()
                        val author = liveAuthors.find { it.id == authorId }
                        val filteredArticles = allArticles.filter { it.authorId == authorId }

                        AuthorDetailScreen(
                            author = author,
                            articles = filteredArticles,
                            onBackClick = { navController.popBackStack() },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // Yearly Archive Screen
                    composable(
                        route = Screen.ArchiveDetail.route,
                        arguments = listOf(navArgument("year") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val year = backStackEntry.arguments?.getInt("year") ?: 2025
                        val allArticles by exploreViewModel.allArticles.collectAsStateWithLifecycle()
                        val liveArchives by exploreViewModel.yearArchives.collectAsStateWithLifecycle()
                        val yearArchive = liveArchives.find { it.year == year }
                        val filteredArticles = allArticles.filter { it.year == year }

                        ArchiveYearDetailScreen(
                            yearArchive = yearArchive,
                            articles = filteredArticles,
                            onBackClick = { navController.popBackStack() },
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }
                }
            }
        }
    }
}
