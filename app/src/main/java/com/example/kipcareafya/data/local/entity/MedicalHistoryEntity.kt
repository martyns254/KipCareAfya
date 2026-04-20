package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_history")
data class MedicalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val conditionName: String,
    val diagnosedDate: String,
    val status: String,
    val medications: String?,
    val notes: String?
)