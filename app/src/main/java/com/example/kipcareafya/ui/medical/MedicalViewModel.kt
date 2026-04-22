package com.example.kipcareafya.ui.medical

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.TimeUnit
import com.example.kipcareafya.data.local.HealthDatabase
import com.example.kipcareafya.data.local.entity.MedicalHistoryEntity
import com.example.kipcareafya.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MedicalUiState(
    val history: List<MedicalHistoryEntity> = emptyList(),
    val medications: List<MedicationEntity> = emptyList(),
    val isLoading: Boolean = true,
    val savedMessage: String? = null,
    val selectedTab: Int = 0  // 0 = History, 1 = Medications
)

class MedicalViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HealthDatabase.getInstance(app)
    private val dao = db.medicalHistoryDao()

    private val _uiState = MutableStateFlow(MedicalUiState())
    val uiState: StateFlow<MedicalUiState> = _uiState.asStateFlow()

    private val userId = 1

    init {
        viewModelScope.launch {
            launch { loadHistory() }
            launch { loadMedications() }
        }
    }

    private suspend fun loadHistory() {
        dao.getHistory(userId).collect { records ->
            _uiState.update { it.copy(history = records, isLoading = false) }
        }
    }

    private suspend fun loadMedications() {
        dao.getMedications(userId).collect { meds ->
            _uiState.update { it.copy(medications = meds) }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun addHistory(
        conditionName: String,
        diagnosedDate: String,
        status: String,
        medications: String,
        notes: String
    ) {
        viewModelScope.launch {
            dao.insertHistory(
                MedicalHistoryEntity(
                    userId = userId,
                    conditionName = conditionName,
                    diagnosedDate = diagnosedDate,
                    status = status,
                    medications = medications.ifBlank { null },
                    notes = notes.ifBlank { null }
                )
            )
            _uiState.update { it.copy(savedMessage = "Record saved!") }
        }
    }

    fun addMedication(
        name: String,
        dosage: String,
        frequency: String,
        notes: String
    ) {
        viewModelScope.launch {
            dao.insertMedication(
                MedicationEntity(
                    userId = userId,
                    name = name,
                    dosage = dosage.ifBlank { null },
                    frequency = frequency.ifBlank { null },
                    startDate = System.currentTimeMillis(),
                    endDate = null,
                    notes = notes.ifBlank { null }
                )
            )
            _uiState.update { it.copy(savedMessage = "Medication saved!") }
        }
    }
    //fun scheduleDailyReminder(
       // context: android.content.Context,
        //medicationName: String,
        //dosage: String,
        //hourOfDay: Int,
        //minute: Int
    //) {
      //  val now = java.util.Calendar.getInstance()
        //val target = java.util.Calendar.getInstance().apply {
          //  set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            //set(java.util.Calendar.MINUTE, minute)
            //set(java.util.Calendar.SECOND, 0)
        //}
      //  if (target.before(now)) target.add(java.util.Calendar.DAY_OF_MONTH, 1)

        //val delay = target.timeInMillis - now.timeInMillis

        //val data = workDataOf(
          //  "medication_name" to medicationName,
            //"dosage" to dosage
        //)

        //val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
          //  24, TimeUnit.HOURS
       // )
         //   .setInitialDelay(delay, TimeUnit.MILLISECONDS)
           // .setInputData(data)
            //.addTag("medication_$medicationName")
           // .build()

        //WorkManager.getInstance(context).enqueueUniquePeriodicWork(
          //  "reminder_$medicationName",
            //ExistingPeriodicWorkPolicy.REPLACE,
            //request
        //)
    //}

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}