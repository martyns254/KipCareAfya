package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val temperature: Float?,
    val symptoms: String?,
    val notes: String?,
    val recordedAt: Long = System.currentTimeMillis()
)