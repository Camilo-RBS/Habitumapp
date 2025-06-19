package com.wayneenterprises.habitum.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayneenterprises.habitum.ui.main.MainScreen
import com.wayneenterprises.habitum.ui.screens.admin.AdminScreen
import com.wayneenterprises.habitum.ui.screens.auth.LoginScreen
import com.wayneenterprises.habitum.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsState()

    // ✅ AGREGADO: Manejo de errores
    LaunchedEffect(uiState) {
        println("🔄 AppNavigation - Estado: isAuth=${uiState.isAuthenticated}, isAdmin=${uiState.isAdmin}, isInit=${uiState.isInitialized}")
    }

    when {
        !uiState.isInitialized -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        !uiState.isAuthenticated -> {
            LoginScreen(viewModel = authViewModel)
        }

        uiState.isAdmin -> {
            AdminScreen(authViewModel = authViewModel)
        }

        else -> {
            MainScreen()
        }
    }
}