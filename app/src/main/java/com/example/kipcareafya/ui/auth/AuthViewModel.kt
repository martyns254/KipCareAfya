package com.example.kipcareafya.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.UserPreferences
import com.example.kipcareafya.data.local.entity.UserEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val userId: Int = -1,
    val username: String = ""
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val userDao = db.userDao()
    private val prefs = UserPreferences(app)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if already logged in
        viewModelScope.launch {
            prefs.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    prefs.userId.first().let { id ->
                        prefs.username.first().let { name ->
                            _uiState.update {
                                it.copy(
                                    isLoggedIn = true,
                                    isLoading = false,
                                    userId = id,
                                    username = name
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            try {
                // Check if username already exists
                val existing = userDao.getUserByUsername(username)
                if (existing != null) {
                    _uiState.update { it.copy(error = "Username already taken") }
                    return@launch
                }
                val user = UserEntity(
                    username = username,
                    email = email,
                    passwordHash = hashPassword(password)
                )
                val id = userDao.insertUser(user).toInt()
                prefs.saveUser(id, username,email)
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        userId = id,
                        username = username,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Registration failed: ${e.message}") }
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter username and password") }
            return
        }
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user == null) {
                _uiState.update { it.copy(error = "User not found") }
                return@launch
            }
            if (!verifyPassword(password, user.passwordHash)) {
                _uiState.update { it.copy(error = "Incorrect password") }
                return@launch
            }
            prefs.saveUser(user.id, user.username, user.email)
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    userId = user.id,
                    username = user.username,
                    error = null
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.clearUser()
            _uiState.update {
                AuthUiState(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Simple hash — in production use BCrypt
    private fun hashPassword(password: String): String {
        return java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}