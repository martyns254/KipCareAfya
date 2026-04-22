package com.example.kipcareafya.ui.trends

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.dao.UserMetricWithType
import com.example.kipcareafya.data.local.entity.MetricTypeEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrendsUiState(
    val metricTypes: List<MetricTypeEntity> = emptyList(),
    val selectedMetricName: String = "weight",
    val trendData: List<UserMetricWithType> = emptyList(),
    val stepCount: Int = 0,
    val stepGoal: Int = 8000,
    val stepSensorAvailable: Boolean = false,
    val selectedPeriod: String = "7d",
    val isLoading: Boolean = true
)

class TrendsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val metricDao = db.userMetricDao()

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val userId = 1

    // Step counter sensor
    private val sensorManager =
        app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // TYPE_STEP_COUNTER gives total steps since boot
            // We store session start and subtract
            val totalSteps = event.values[0].toInt()
            _uiState.update { it.copy(stepCount = totalSteps % 100000) }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        checkStepSensor()
        viewModelScope.launch {
            launch { loadMetricTypes() }
            launch { loadTrendData() }
        }
    }

    private fun checkStepSensor() {
        val available = stepSensor != null
        _uiState.update { it.copy(stepSensorAvailable = available) }
        if (available) {
            sensorManager.registerListener(
                stepListener,
                stepSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private suspend fun loadMetricTypes() {
        metricDao.getAllMetricTypes().collect { types ->
            _uiState.update { it.copy(metricTypes = types, isLoading = false) }
        }
    }

    private suspend fun loadTrendData() {
        val since = sinceMillis(_uiState.value.selectedPeriod)
        val metricType = _uiState.value.metricTypes
            .find { it.name == _uiState.value.selectedMetricName }
        if (metricType != null) {
            metricDao.getMetricsWithType(userId, since).collect { entries ->
                _uiState.update {
                    it.copy(
                        trendData = entries.filter { e ->
                            e.name == it.selectedMetricName
                        }
                    )
                }
            }
        }
    }

    fun selectMetric(metricName: String) {
        _uiState.update { it.copy(selectedMetricName = metricName) }
        viewModelScope.launch { loadTrendData() }
    }

    fun selectPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        viewModelScope.launch { loadTrendData() }
    }

    fun setStepGoal(goal: Int) {
        _uiState.update { it.copy(stepGoal = goal) }
    }

    private fun sinceMillis(period: String): Long {
        val days = when (period) {
            "7d"  -> 7L
            "30d" -> 30L
            "90d" -> 90L
            "1y"  -> 365L
            else  -> 30L
        }
        return System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
    }

    override fun onCleared() {
        super.onCleared()
        if (stepSensor != null) {
            sensorManager.unregisterListener(stepListener)
        }
    }
}