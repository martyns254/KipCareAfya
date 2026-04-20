package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptoms_log")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val symptomName: String,
    val severity: String,
    val duration: String,
    val notes: String?,
    val recordedAt: Long = System.currentTimeMillis()
)