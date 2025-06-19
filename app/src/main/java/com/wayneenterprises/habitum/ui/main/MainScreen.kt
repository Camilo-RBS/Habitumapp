package com.wayneenterprises.habitum.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.wayneenterprises.habitum.navigation.BottomBar
import com.wayneenterprises.habitum.navigation.NavGraph
import com.wayneenterprises.habitum.viewmodel.AuthViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel() // ← Solo agregar esta línea

    Scaffold(
        bottomBar = {
            BottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            authViewModel = authViewModel, // ← Y pasar el authViewModel aquí
            modifier = Modifier.padding(innerPadding)
        )
    }
}