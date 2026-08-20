package com.sricharan.dailyschedule.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sricharan.dailyschedule.ui.screens.AddEditScreen
import com.sricharan.dailyschedule.ui.screens.GardenScreen
import com.sricharan.dailyschedule.ui.screens.HomeScreen
import com.sricharan.dailyschedule.ui.screens.MonthScreen
import com.sricharan.dailyschedule.ui.screens.SettingsScreen
import com.sricharan.dailyschedule.viewmodel.ScheduleViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Shared across screens so the selected day stays put when moving between
    // the week strip, the month view, and back.
    val viewModel: ScheduleViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate("edit/-1") },
                onItemClick = { id -> navController.navigate("edit/$id") },
                onSettingsClick = { navController.navigate("settings") },
                onMonthClick = { navController.navigate("month") },
                onGardenClick = { navController.navigate("garden") }
            )
        }
        composable("month") {
            MonthScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onItemClick = { id -> navController.navigate("edit/$id") }
            )
        }
        composable("garden") {
            GardenScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "edit/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            AddEditScreen(
                viewModel = viewModel,
                existingItemId = if (itemId == -1L) null else itemId,
                onDone = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
