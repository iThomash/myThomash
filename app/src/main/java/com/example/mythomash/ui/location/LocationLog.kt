package com.example.mythomash.ui.location

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_logs")
data class LocationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val pressure: Double,
    val gravityX: Double?,
    val gravityY: Double?,
    val gravityZ: Double?
)