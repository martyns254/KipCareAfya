package com.example.kipcareafya.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.entity.HealthGoalEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalsUiState(
    val activeGoals: List<HealthGoalEntity> = emptyList(),
    val completedGoals: List<HealthGoalEntity> = emptyList(),
    val isLoading: Boolean = true,
    val savedMessage: String? = null,
    val selectedTab: Int = 0
)

class GoalsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val dao = db.goalDao()

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        viewModelScope.launch {
            launch { loadActiveGoals() }
            launch { loadCompletedGoals() }
        }
    }

    private suspend fun loadActiveGoals() {
        dao.getActiveGoals(userId).collect { goals ->
            _uiState.update { it.copy(activeGoals = goals, isLoading = false) }
        }
    }

    private suspend fun loadCompletedGoals() {
        dao.getGoals(userId).collect { goals ->
            _uiState.update {
                it.copy(completedGoals = goals.filter { g -> g.isCompleted })
            }
        }
    }

    fun addGoal(
        title: String,
        metricType: String,
        targetValue: Float?,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            dao.insert(
                HealthGoalEntity(
                    userId = userId,
                    title = title,
                    metricType = metricType.ifBlank { null },
                    targetValue = targetValue,
                    currentValue = null,
                    targetDate = targetDate,
                    isCompleted = false
                )
            )
            _uiState.update { it.copy(savedMessage = "Goal added!") }
        }
    }

    fun markComplete(goal: HealthGoalEntity) {
        viewModelScope.launch {
            dao.update(goal.copy(isCompleted = true))
        }
    }

    fun deleteGoal(goal: HealthGoalEntity) {
        viewModelScope.launch {
            dao.delete(goal)
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}