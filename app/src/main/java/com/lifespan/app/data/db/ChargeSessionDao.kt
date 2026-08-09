package com.lifespan.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Aggregate row backing the battery-health card; see [ChargeSessionDao.observeStats]. */
data class SessionStatsRow(
    val sessionCount: Int,
    val percentAdded: Long?,
    val mahAdded: Double?,
    val avgPeakTemp: Double?,
    val maxPeakTemp: Double?,
    val hotCount: Int,
)

@Dao
interface ChargeSessionDao {

    @Insert
    suspend fun insert(session: ChargeSessionEntity): Long

    @Update
    suspend fun update(session: ChargeSessionEntity)

    @Query("SELECT * FROM charge_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<ChargeSessionEntity?>

    @Query("SELECT * FROM charge_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<ChargeSessionEntity>>

    @Query(
        """
        SELECT COUNT(*) AS sessionCount,
               SUM(CASE WHEN endLevel IS NOT NULL AND endLevel > startLevel
                        THEN endLevel - startLevel ELSE 0 END) AS percentAdded,
               SUM(COALESCE(totalMahAdded, 0)) AS mahAdded,
               AVG(peakTempCelsius) AS avgPeakTemp,
               MAX(peakTempCelsius) AS maxPeakTemp,
               SUM(CASE WHEN peakTempCelsius >= :hotThreshold THEN 1 ELSE 0 END) AS hotCount
        FROM charge_sessions
        """,
    )
    fun observeStats(hotThreshold: Double): Flow<SessionStatsRow>

    @Query("DELETE FROM charge_sessions")
    suspend fun clear()
}
