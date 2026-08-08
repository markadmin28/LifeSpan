package com.lifespan.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryLogDao {

    @Insert
    suspend fun insert(log: TelemetryLogEntity): Long

    @Query("SELECT * FROM battery_telemetry_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeForSession(sessionId: Long): Flow<List<TelemetryLogEntity>>

    @Query("SELECT COUNT(*) FROM battery_telemetry_logs WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("SELECT * FROM battery_telemetry_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<TelemetryLogEntity>>
}
