package com.lifespan.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charge_sessions")
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startLevel: Int,
    val endLevel: Int? = null,
    val plugType: String, // AC, USB, Wireless
    val peakTempCelsius: Double,
    val peakCurrentMa: Double,
    val totalMahAdded: Double? = null,
)
