package com.example.kipcareafya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String = "",
    val dateOfBirth: String? = null,
    val heightCm: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)