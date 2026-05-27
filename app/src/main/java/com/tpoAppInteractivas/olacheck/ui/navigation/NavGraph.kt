package com.tpoAppInteractivas.olacheck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tpoAppInteractivas.olacheck.ui.screens.BeachDetailScreen
import com.tpoAppInteractivas.olacheck.ui.screens.HomeScreen
import com.tpoAppInteractivas.olacheck.ui.screens.LoginScreen
import com.tpoAppInteractivas.olacheck.ui.screens.ProfileScreen
import com.tpoAppInteractivas.olacheck.ui.screens.SplashScreen
import androidx.navigation.navArgument
import com.tpoAppInteractivas.olacheck.ui.screens.BeachDetailScreen
import com.tpoAppInteractivas.olacheck.ui.screens.ProfileScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val DETAIL = "detail/{beachId}"
    const val PROFILE = "profile"

    fun detailRoute(beachId: String) = "detail/$beachId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDetail = { beachId ->
                    navController.navigate(Routes.detailRoute(beachId))
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("beachId") { type = NavType.StringType })
        ) {
            BeachDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}