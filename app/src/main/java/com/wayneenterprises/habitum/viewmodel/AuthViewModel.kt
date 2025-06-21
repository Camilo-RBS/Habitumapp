package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayneenterprises.habitum.model.User
import com.wayneenterprises.habitum.repository.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
    val isInitialized: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repository = SupabaseRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        initializeAuth()
    }

    private fun initializeAuth() {
        viewModelScope.launch {
            // Crear usuario admin si no existe
            repository.createAdminUserIfNeeded()

            // Verificar si hay un usuario autenticado
            checkCurrentUser()

            _uiState.value = _uiState.value.copy(isInitialized = true)
        }
    }

    private suspend fun checkCurrentUser() {
        repository.getCurrentUser().fold(
            onSuccess = { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        isAdmin = user.userType == "admin",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = false,
                        currentUser = null,
                        isAdmin = false
                    )
                }
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    isAdmin = false
                )
            }
        )
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.signIn(email, password).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = user,
                        isAdmin = user.userType == "admin",
                        errorMessage = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al iniciar sesión"
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.signUp(email, password, name).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = user,
                        isAdmin = user.userType == "admin",
                        errorMessage = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al registrarse"
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                repository.signOut()

                _uiState.value = AuthUiState(
                    isLoading = false,
                    isAuthenticated = false,
                    currentUser = null,
                    errorMessage = null,
                    isAdmin = false,
                    isInitialized = true
                )

                println("🚪 Sesión cerrada correctamente")

            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isAuthenticated = false,
                    currentUser = null,
                    errorMessage = "Error al cerrar sesión, pero sesión local limpiada",
                    isAdmin = false,
                    isInitialized = true
                )

                println("⚠️ Error cerrando sesión: ${e.message}, pero estado limpiado")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

}