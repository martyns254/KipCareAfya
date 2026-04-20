package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_categories")
data class SymptomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)