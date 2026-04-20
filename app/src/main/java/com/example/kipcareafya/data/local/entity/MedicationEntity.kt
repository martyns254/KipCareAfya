package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val dosage: String?,
    val frequency: String?,
    val startDate: Long?,
    val endDate: Long?,
    val notes: String?
)