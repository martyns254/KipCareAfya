package com.example.kipcareafya.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.UserPreferences
import com.example.kipcareafya.data.local.dataStore
import com.example.kipcareafya.data.local.entity.UserEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProfileUiState(
    val userId: Int = -1,
    val username: String = "",
    val email: String = "",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val totalMetricsLogged: Int = 0,
    val totalSymptomsLogged: Int = 0,
    val totalGoals: Int = 0,
    val memberSince: String = "",
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = UserPreferences(app)
    private val db = HealthDatabase.getInstance(app)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.userId,
                prefs.username,
                prefs.userEmail,
                prefs.isDarkMode,
                prefs.notificationsEnabled
            ) { id, name, email, dark, notifs ->
                ProfileUiState(
                    userId = id,
                    username = name,
                    email = email,
                    isDarkMode = dark,
                    notificationsEnabled = notifs,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
                if (state.userId > 0) loadStats(state.userId)
            }
        }
    }

    private fun loadStats(userId: Int) {
        viewModelScope.launch {
            val since = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)

            db.userMetricDao().getMetricsWithType(userId, since).collect { metrics ->
                _uiState.update { it.copy(totalMetricsLogged = metrics.size) }
            }
        }
        viewModelScope.launch {
            db.goalDao().getGoals(userId).collect { goals ->
                _uiState.update { it.copy(totalGoals = goals.size) }
            }
        }
        viewModelScope.launch {
            val user = db.userDao().getUserById(userId)
            if (user != null) {
                val date = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(Date(user.createdAt))
                _uiState.update { it.copy(memberSince = date) }
            }
        }
    }

    fun updateProfile(newUsername: String, newEmail: String) {
        viewModelScope.launch {
            try {
                val userId = _uiState.value.userId
                val user = db.userDao().getUserById(userId) ?: return@launch
                db.userDao().updateUser(user.copy(
                    username = newUsername.trim(),
                    email = newEmail.trim()
                ))
                prefs.saveUser(userId, newUsername.trim(), newEmail.trim())
                _uiState.update { it.copy(saveSuccess = true, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update profile") }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotifications(enabled)
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun exportHealthData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val userId = _uiState.value.userId
                val since = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
                val metrics = db.userMetricDao()
                    .getMetricsWithType(userId, since).first()

                val sb = StringBuilder()
                sb.appendLine("KipCare Afya Health Export")
                sb.appendLine("User: ${_uiState.value.username}")
                sb.appendLine("Exported: ${Date()}")
                sb.appendLine("=".repeat(40))
                sb.appendLine()
                sb.appendLine("HEALTH METRICS (${metrics.size} records)")
                sb.appendLine("-".repeat(40))

                metrics.forEach { m ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm",
                        Locale.getDefault()).format(Date(m.recordedAt))
                    sb.appendLine("$date | ${m.displayName}: ${m.value} ${m.unit ?: ""}")
                }

                // Write to Downloads folder
                val fileName = "kipcareafya_export_${System.currentTimeMillis()}.txt"
                val file = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    fileName
                )
                file.writeText(sb.toString())

                _uiState.update { it.copy(saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            val user = db.userDao().getUserById(userId)
            if (user == null) {
                onResult(false, "User not found")
                return@launch
            }

            // Verify current password
            val currentHash = hashPassword(currentPassword)
            if (currentHash != user.passwordHash) {
                onResult(false, "Current password is incorrect")
                return@launch
            }

            if (newPassword.length < 6) {
                onResult(false, "New password must be at least 6 characters")
                return@launch
            }

            // Save new password
            db.userDao().updateUser(user.copy(passwordHash = hashPassword(newPassword)))
            onResult(true, "Password updated successfully")
        }
    }

    private fun hashPassword(password: String): String {
        return java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            // Delete all user data from Room
            db.userDao().getUserById(userId)?.let { user ->
                // Room cascades deletes via ForeignKey if set up
                // For safety, manually clear everything
                db.goalDao().getGoals(userId).first().forEach {
                    db.goalDao().delete(it)
                }
                db.medicalHistoryDao().getHistory(userId).first().forEach {
                    db.medicalHistoryDao().deleteHistory(it)
                }
                // Clear preferences
                prefs.clearUser()
            }
            onComplete()
        }
    }
}