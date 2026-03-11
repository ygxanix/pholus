package com.pholus.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pholus.ui.screens.chat.ChatScreen
import com.pholus.ui.screens.curl.CurlConverterScreen
import com.pholus.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Chat : Screen("chat?conversationId={conversationId}") {
        fun createRoute(conversationId: String? = null): String {
            return if (conversationId != null) "chat?conversationId=$conversationId" else "chat"
        }
    }
    data object Settings : Screen("settings")
    data object CurlConverter : Screen("curl_converter")
    data object ModelConfig : Screen("model_config")
    data object Profiles : Screen("profiles")
}

@Composable
fun PholusNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Chat.createRoute()
    ) {
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            ChatScreen(
                conversationId = conversationId,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCurl = { navController.navigate(Screen.CurlConverter.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CurlConverter.route) {
            CurlConverterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
