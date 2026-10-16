package com.everettpenne.sietch.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.everettpenne.glass.*
import com.everettpenne.sietch.BuildConfig
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.Navigator
import com.everettpenne.sietch.ui.screens.*
import com.everettpenne.sietch.ui.theme.SietchTheme
import com.everettpenne.sietch.util.RouteCodec
import com.kyant.backdrop.backdrops.layerBackdrop

object Routes {
    const val PLACES = "places"
    const val RECENTS = "recents"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val BROWSE = "browse/{token}"
    fun browse(treeUri: String, documentId: String, name: String) = "browse/${RouteCodec.folder(treeUri, documentId, name)}"
}

@Composable
fun SietchRoot() {
    SietchTheme {
        var viewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
        val context = LocalContext.current
        val graph = context.graph
        val updater = com.everettpenne.updater.Updater.get(context, com.everettpenne.updater.UpdateConfig("everettpenne/Sietch", BuildConfig.VERSION_NAME))
        // Debug builds skip the launch check so tests never meet a dialog; Settings > Check for Updates still works there.
        LaunchedEffect(Unit) { if (!BuildConfig.DEBUG) updater.checkOnLaunch() }
        com.everettpenne.updater.UpdateDialog(updater, "Sietch")

        val navController = rememberNavController()
        val nav = remember(navController) {
            object : Navigator {
                override fun openFolder(treeUri: String, documentId: String, name: String) = navController.navigate(Routes.browse(treeUri, documentId, name))
                override fun openSearch() = navController.navigate(Routes.SEARCH) { popUpTo(Routes.PLACES) { inclusive = false }; launchSingleTop = true }
                override fun openSettings() = navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
            }
        }

        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val tabs = Tab.entries.associateBy { it.route }
        var lastTab by remember { mutableStateOf(Tab.Places) }
        LaunchedEffect(route) { tabs[route]?.let { lastTab = it } }
        val tabBar: (@Composable () -> Unit)? = if (route == Routes.BROWSE || route == Routes.SETTINGS) null else ({
            GlassTabBar(Tab.bar, selected = lastTab, search = Tab.Search, onSelect = { tab ->
                navController.navigate(tab.route) { popUpTo(Routes.PLACES) { inclusive = false }; launchSingleTop = true }
            })
        })

        CompositionLocalProvider(com.everettpenne.updater.LocalUpdater provides updater, com.everettpenne.media.LocalImageViewer provides { urls, i -> viewer = urls to i }, LocalTabBar provides tabBar) {
            // The plain page colour is also a glass backdrop, recorded in a sibling of the screens: recording the screens themselves would make any
            // glass control inside them sample a layer that contains it, which sends the renderer into endless recursion.
            val pageBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop).background(MaterialTheme.colorScheme.background))
                CompositionLocalProvider(LocalGlassBackdrop provides pageBackdrop) {
                    NavHost(navController, startDestination = Routes.PLACES, modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn() }, exitTransition = { fadeOut() }, popEnterTransition = { fadeIn() }, popExitTransition = { fadeOut() }) {
                        composable(Routes.PLACES) { PlacesScreen(nav) }
                        composable(Routes.RECENTS) { RecentsScreen(nav) }
                        composable(Routes.SEARCH) { SearchScreen(nav, onBack = { navController.popBackStack() }) }
                        composable(Routes.SETTINGS) { SettingsScreen(nav, onBack = { navController.popBackStack() }) }
                        composable(Routes.BROWSE) { e -> BrowseScreen(e.arguments?.getString("token").orEmpty(), nav, onBack = { navController.popBackStack() }) }
                    }
                }
            }
            viewer?.let { (urls, i) -> com.everettpenne.media.ImageViewer(urls, i) { viewer = null } }
        }
    }
}
