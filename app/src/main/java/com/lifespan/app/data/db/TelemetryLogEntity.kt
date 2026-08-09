package com.lifespan.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_telemetry_logs",
    foreignKeys = [
        ForeignKey(
            entity = ChargeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val timestamp: Long,
    val batteryLevel: Int,
    val voltageMv: Int,
    val currentMa: Int,
    val temperatureCelsius: Double,
    val isCharging: Boolean,
)
