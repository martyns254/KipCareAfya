package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_recommendations")
data class SymptomRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symptomCombination: String,
    val severityLevel: String,
    val recommendation: String,
    val warningSigns: String?,
    val basicTreatment: String?,
    val seekDoctorIf: String?,
    val emergencyLevel: String
)