package com.example.kipcareafya.data.local.dao

import androidx.room.*
import com.example.kipcareafya.data.local.entity.MedicalHistoryEntity
import com.example.kipcareafya.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: MedicalHistoryEntity)

    @Delete
    suspend fun deleteHistory(record: MedicalHistoryEntity)

    @Query("""
        SELECT * FROM medical_history
        WHERE userId = :userId
        ORDER BY diagnosedDate DESC
    """)
    fun getHistory(userId: Int): Flow<List<MedicalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE userId = :userId")
    fun getMedications(userId: Int): Flow<List<MedicationEntity>>
}