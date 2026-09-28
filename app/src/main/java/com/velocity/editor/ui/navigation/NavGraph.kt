package com.velocity.editor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.velocity.editor.ui.editor.EditorScreen
import com.velocity.editor.ui.export.ExportScreen
import com.velocity.editor.ui.home.MainScreen
import com.velocity.editor.ui.library.LibraryScreen

object Routes {
    const val MAIN = "main"
    const val EDITOR = "editor/{projectId}"
    const val LIBRARY = "library/{projectId}/{tab}"
    const val EXPORT = "export/{projectId}"
    fun editor(id: Long) = "editor/$id"
    fun library(id: Long, tab: Int) = "library/$id/$tab"
    fun export(id: Long) = "export/$id"
}

@Composable
fun VelocityNavHost() {
    val nav = rememberNavController()
    val projectArg = navArgument("projectId") { type = NavType.LongType }

    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(onOpenProject = { nav.navigate(Routes.editor(it)) })
        }
        composable(Routes.EDITOR, arguments = listOf(projectArg)) {
            EditorScreen(
                onBack = { nav.popBackStack() },
                onExport = { nav.navigate(Routes.export(it)) },
                onOpenLibrary = { id, tab -> nav.navigate(Routes.library(id, tab)) },
            )
        }
        composable(
            Routes.LIBRARY,
            arguments = listOf(projectArg, navArgument("tab") { type = NavType.IntType }),
        ) { entry ->
            LibraryScreen(
                initialTab = entry.arguments?.getInt("tab") ?: 0,
                onBack = { nav.popBackStack() },
                onExport = { nav.navigate(Routes.export(it)) },
            )
        }
        composable(Routes.EXPORT, arguments = listOf(projectArg)) {
            ExportScreen(onBack = { nav.popBackStack() })
        }
    }
}
