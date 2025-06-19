package com.wayneenterprises.habitum.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wayneenterprises.habitum.ui.screens.HomeScreen
import com.wayneenterprises.habitum.ui.screens.ReminderScreen
import com.wayneenterprises.habitum.ui.screens.TaskScreen
import com.wayneenterprises.habitum.ui.screens.physical.ActivityMenuScreen
import com.wayneenterprises.habitum.ui.screens.physical.RunMapScreen
import com.wayneenterprises.habitum.ui.screens.physical.WalkingScreen
import com.wayneenterprises.habitum.viewmodel.AuthViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // MANTENER LA RUTA ORIGINAL DE TASKS (para navegación normal)
        composable(Screen.Tasks.route) {
            TaskScreen(
                selectedCategory = "", // Sin categoría específica
                authViewModel = authViewModel
            )
        }

        // NUEVA RUTA ESPECÍFICA PARA TASKS CON CATEGORÍA
        composable(
            route = "task_detail?selectedCategory={selectedCategory}",
            arguments = listOf(
                navArgument("selectedCategory") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val selectedCategory = backStackEntry.arguments?.getString("selectedCategory") ?: ""
            TaskScreen(
                selectedCategory = selectedCategory,
                authViewModel = authViewModel
            )
        }

        // MANTENER LA RUTA ORIGINAL DE REMINDERS (para navegación normal)
        composable(Screen.Reminders.route) {
            ReminderScreen(
                selectedReminderId = "", // Sin ID específico
                authViewModel = authViewModel
            )
        }

        // NUEVA RUTA ESPECÍFICA PARA REMINDERS CON PARÁMETRO (mantener como estaba)
        composable(
            route = "reminder_detail?selectedReminderId={selectedReminderId}",
            arguments = listOf(
                navArgument("selectedReminderId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val selectedReminderId = backStackEntry.arguments?.getString("selectedReminderId") ?: ""
            ReminderScreen(
                selectedReminderId = selectedReminderId,
                authViewModel = authViewModel
            )
        }

        composable(Screen.Physical.route) {
            ActivityMenuScreen(
                navController = navController,
            )
        }
        composable("run_tracking") {
            RunMapScreen()
        }
        composable("walking_screen") {
            WalkingScreen()
        }
    }
}