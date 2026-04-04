package com.example.bandmapper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "band_data")
data class BandData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val bandName: String,
    val technology: String, // LTE, 5G, etc.
    val timestamp: Long = System.currentTimeMillis()
)
