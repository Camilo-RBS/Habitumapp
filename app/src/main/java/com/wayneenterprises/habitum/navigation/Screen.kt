package com.wayneenterprises.habitum.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Inicio")
    object Tasks : Screen("tasks", "Tareas")
    object Reminders : Screen("reminders", "Recordatorios")
    object Physical : Screen("activity_menu", "Actividad Física")
}
