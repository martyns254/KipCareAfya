package com.example.kipcareafya.ui.symptoms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.entity.SymptomCategoryEntity
import com.example.kipcareafya.data.local.entity.SymptomEntity
import com.example.kipcareafya.data.local.entity.SymptomLogEntity
import com.example.kipcareafya.data.local.entity.SymptomRecommendationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SymptomUiState(
    val categories: List<SymptomCategoryEntity> = emptyList(),
    val symptomsByCategory: Map<Int, List<SymptomEntity>> = emptyMap(),
    val selectedSymptoms: Set<String> = emptySet(),
    val severity: String = "mild",
    val analysisResult: SymptomRecommendationEntity? = null,
    val showDefaultResult: Boolean = false,
    val isAnalysing: Boolean = false,
    val isLoading: Boolean = true,
    val justSaved: Boolean = false
)

class SymptomViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val dao = db.symptomDao()

    private val _uiState = MutableStateFlow(SymptomUiState())
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        loadCatalogue()
    }

    private fun loadCatalogue() {
        viewModelScope.launch {
            dao.getCategories().collect { categories ->
                val symptomMap = mutableMapOf<Int, List<SymptomEntity>>()
                categories.forEach { cat ->
                    dao.getSymptomsForCategory(cat.id).first().let { symptoms ->
                        symptomMap[cat.id] = symptoms
                    }
                }
                _uiState.update {
                    it.copy(
                        categories = categories,
                        symptomsByCategory = symptomMap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleSymptom(symptomName: String) {
        _uiState.update { state ->
            val updated = state.selectedSymptoms.toMutableSet()
            if (symptomName in updated) updated.remove(symptomName)
            else updated.add(symptomName)
            state.copy(
                selectedSymptoms = updated,
                // Clear previous result when selection changes
                analysisResult = null,
                showDefaultResult = false
            )
        }
    }

    fun setSeverity(severity: String) {
        _uiState.update {
            it.copy(
                severity = severity,
                analysisResult = null,
                showDefaultResult = false
            )
        }
    }

    fun analyseSymptoms() {
        val state = _uiState.value
        if (state.selectedSymptoms.isEmpty()) return

        _uiState.update { it.copy(isAnalysing = true) }

        viewModelScope.launch {
            val sorted = state.selectedSymptoms.sorted()
            val combo = sorted.joinToString(",")
            val severity = state.severity

            // Step 1 — exact match
            var result = dao.findExactRecommendation(combo, severity)

            // Step 2 — partial match per symptom
            if (result == null) {
                for (symptom in sorted) {
                    result = dao.findPartialRecommendation(symptom, severity)
                    if (result != null) break
                }
            }

            // Apply severity override — mirrors backend logic exactly
            if (result != null && severity == "severe") {
                result = result.copy(
                    emergencyLevel = "high",
                    recommendation = "SEEK IMMEDIATE MEDICAL ATTENTION. ${result.recommendation}"
                )
            }

            // Log the symptom session
            sorted.forEach { symptomName ->
                dao.insertLog(
                    SymptomLogEntity(
                        userId = userId,
                        symptomName = symptomName,
                        severity = severity,
                        duration = "unknown",
                        notes = null
                    )
                )
            }

            _uiState.update {
                it.copy(
                    analysisResult = result,
                    showDefaultResult = result == null,
                    isAnalysing = false,
                    justSaved = true
                )
            }
        }
    }

    fun clearResults() {
        _uiState.update {
            it.copy(
                selectedSymptoms = emptySet(),
                severity = "mild",
                analysisResult = null,
                showDefaultResult = false,
                justSaved = false
            )
        }
    }
}