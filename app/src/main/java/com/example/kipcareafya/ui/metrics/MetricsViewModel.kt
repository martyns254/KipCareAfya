package com.example.kipcareafya.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import com.example.kipcareafya.data.local.entity.MetricTypeEntity
import com.example.kipcareafya.data.local.entity.UserMetricEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MetricUiState(
    val metricTypes: List<MetricTypeEntity> = emptyList(),
    val recentEntries: List<UserMetricWithType> = emptyList(),
    val isLoading: Boolean = true,
    val savedMessage: String? = null
)

class MetricsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val dao = db.userMetricDao()

    private val _uiState = MutableStateFlow(MetricUiState())
    val uiState: StateFlow<MetricUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        loadMetricTypes()
        loadRecentEntries()
    }

    private fun loadMetricTypes() {
        viewModelScope.launch {
            dao.getAllMetricTypes().collect { types ->
                _uiState.update { it.copy(metricTypes = types, isLoading = false) }
            }
        }
    }

    private fun loadRecentEntries() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        viewModelScope.launch {
            dao.getMetricsWithType(userId, thirtyDaysAgo).collect { entries ->
                _uiState.update { it.copy(recentEntries = entries) }
            }
        }
    }

    fun getLatestForType(metricName: String): UserMetricWithType? {
        return _uiState.value.recentEntries
            .filter { entry -> entry.name == metricName }
            .maxByOrNull { entry -> entry.recordedAt }
    }

    fun saveMetric(metricId: Int, value: Float, notes: String) {
        viewModelScope.launch {
            dao.insertUserMetric(
                UserMetricEntity(
                    userId = userId,
                    metricId = metricId,
                    value = value,
                    notes = notes.ifBlank { null },
                    recordedAt = System.currentTimeMillis()
                )
            )
            autoCalculateBmi(metricId, value)
            _uiState.update { it.copy(savedMessage = "Record saved!") }
        }
    }

    private suspend fun autoCalculateBmi(savedMetricId: Int, savedValue: Float) {
        val types = _uiState.value.metricTypes
        val weightType = types.find { it.name == "weight" } ?: return
        val heightType = types.find { it.name == "height" } ?: return
        val bmiType    = types.find { it.name == "bmi" }    ?: return

        if (savedMetricId != weightType.id && savedMetricId != heightType.id) return

        val since = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)

        val weight: Float? = if (savedMetricId == weightType.id) savedValue
        else dao.getLatestValue(userId, weightType.id, since)

        val heightCm: Float? = if (savedMetricId == heightType.id) savedValue
        else dao.getLatestValue(userId, heightType.id, since)

        if (weight != null && heightCm != null && heightCm > 0f) {
            val heightM = heightCm / 100f
            val bmi = weight / (heightM * heightM)
            val rounded = String.format("%.1f", bmi).toFloat()
            dao.insertUserMetric(
                UserMetricEntity(
                    userId = userId,
                    metricId = bmiType.id,
                    value = rounded,
                    notes = "Auto-calculated from weight & height",
                    recordedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}