package com.everettpenne.sietch.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.everettpenne.sietch.BuildConfig
import com.everettpenne.sietch.data.Category
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.Navigator
import com.everettpenne.sietch.ui.screens.*
import com.everettpenne.sietch.ui.theme.SietchTheme
import com.everettpenne.glass.*
import com.kyant.backdrop.backdrops.layerBackdrop

object Routes {
    const val HOME = "home"
    const val RECENTS = "recents"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val BROWSE = "browse/{path}"
    const val CATEGORY = "category/{name}"
    fun browse(path: String) = "browse/${Uri.encode(path)}"
    fun category(c: Category) = "category/${c.name}"
}

@Composable
fun SietchRoot(deepLink: Uri? = null, onDeepLinkHandled: () -> Unit = {}) {
    SietchTheme {
        var viewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
        val context = LocalContext.current
        val updater = com.everettpenne.updater.Updater.get(context, com.everettpenne.updater.UpdateConfig("everettpenne/Sietch", BuildConfig.VERSION_NAME))
        // Debug builds skip the launch check so tests never meet a dialog; Settings > Check for Updates still works there.
        LaunchedEffect(Unit) { if (!BuildConfig.DEBUG) updater.checkOnLaunch() }
        com.everettpenne.updater.UpdateDialog(updater, "Sietch")

        val navController = rememberNavController()
        // A launcher shortcut's own address (sietch://search, sietch://trash).
        LaunchedEffect(deepLink) {
            val link = deepLink ?: return@LaunchedEffect
            when (link.host) {
                "search" -> navController.navigate(Routes.SEARCH) { launchSingleTop = true }
                "recents" -> navController.navigate(Routes.RECENTS) { launchSingleTop = true }
                "trash" -> navController.navigate(Routes.TRASH) { launchSingleTop = true }
            }
            onDeepLinkHandled()
        }
        val nav = remember(navController) {
            object : Navigator {
                override fun openFolder(path: String) = navController.navigate(Routes.browse(path))
                override fun backTo(path: String) {
                    fun isTarget(e: NavBackStackEntry?) = e?.destination?.route == Routes.BROWSE && e.arguments?.getString("path") == path
                    // Every folder screen shares one destination; pop one at a time until the wanted folder is on top. If it was never on the stack
                    // (the user came in through a shortcut), open it instead of emptying the stack.
                    val onStack = runCatching { navController.getBackStackEntry(Routes.browse(path)) }.isSuccess
                    if (!onStack) { navController.navigate(Routes.browse(path)); return }
                    while (!isTarget(navController.currentBackStackEntry) && navController.previousBackStackEntry != null) navController.popBackStack()
                }
                override fun openCategory(category: Category) = navController.navigate(Routes.category(category))
                override fun openTrash() = navController.navigate(Routes.TRASH) { launchSingleTop = true }
                override fun openSearch() = navController.navigate(Routes.SEARCH) { launchSingleTop = true }
                override fun openSettings() = navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
            }
        }

        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val tabs = Tab.entries.associateBy { it.route }
        var lastTab by remember { mutableStateOf(Tab.Home) }
        LaunchedEffect(route) { tabs[route]?.let { lastTab = it } }
        val tabBar: (@Composable () -> Unit)? = if (route != null && route !in tabs) null else ({
            GlassTabBar(Tab.bar, selected = lastTab, search = Tab.Search, onSelect = { tab ->
                navController.navigate(tab.route) { popUpTo(Routes.HOME) { inclusive = false }; launchSingleTop = true }
            })
        })

        CompositionLocalProvider(com.everettpenne.updater.LocalUpdater provides updater, com.everettpenne.media.LocalImageViewer provides { urls, i -> viewer = urls to i }, LocalTabBar provides tabBar) {
        // Saved web pages and web shortcuts open in the suite's browser (Glass), over the app; video and sound in Glass's player.
        com.everettpenne.browser.BrowserHost {
        com.everettpenne.media.VideoViewerHost {
            // The plain page colour is also a glass backdrop, recorded in a sibling of the screens: recording the screens themselves would make any
            // glass control inside them sample a layer that contains it, which sends the renderer into endless recursion.
            val pageBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop).background(MaterialTheme.colorScheme.background))
                CompositionLocalProvider(LocalGlassBackdrop provides pageBackdrop) {
                    NavHost(navController, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn() }, exitTransition = { fadeOut() }, popEnterTransition = { fadeIn() }, popExitTransition = { fadeOut() }) {
                        composable(Routes.HOME) { HomeScreen(nav, openRecents = { navController.navigate(Routes.RECENTS) { popUpTo(Routes.HOME); launchSingleTop = true } }) }
                        composable(Routes.RECENTS) { RecentsScreen(nav) }
                        composable(Routes.SEARCH) { SearchScreen(nav, onBack = { navController.popBackStack() }, showBack = navController.previousBackStackEntry?.destination?.route == Routes.BROWSE) }
                        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
                        composable(Routes.TRASH) { TrashScreen(onBack = { navController.popBackStack() }) }
                        composable(Routes.BROWSE) { e -> BrowseScreen(e.arguments?.getString("path").orEmpty().ifEmpty { context.graph.root.path }, nav, onBack = { navController.popBackStack() }) }
                        composable(Routes.CATEGORY) { e ->
                            val c = Category.entries.firstOrNull { it.name == e.arguments?.getString("name") } ?: Category.IMAGES
                            CategoryScreen(c, nav, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
            // In picture-in-picture the small window shows only the video.
            if (com.everettpenne.media.VideoPip.active) com.everettpenne.media.VideoPip.player?.let {
                com.everettpenne.media.VideoSurface(it, Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
            }
            viewer?.let { (urls, i) -> com.everettpenne.media.ImageViewer(urls, i) { viewer = null } }
        }
        }
        }
    }
}
