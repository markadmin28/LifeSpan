package com.lifespan.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeSessionDao {

    @Insert
    suspend fun insert(session: ChargeSessionEntity): Long

    @Update
    suspend fun update(session: ChargeSessionEntity)

    @Query("SELECT * FROM charge_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<ChargeSessionEntity>>

    @Query("SELECT * FROM charge_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<ChargeSessionEntity?>

    /** Finished sessions that measured how much charge went in — the input to capacity estimates. */
    @Query(
        """
        SELECT * FROM charge_sessions
        WHERE endTime IS NOT NULL AND endLevel IS NOT NULL AND totalMahAdded IS NOT NULL
        ORDER BY startTime DESC LIMIT :limit
        """,
    )
    fun observeCompleted(limit: Int = 60): Flow<List<ChargeSessionEntity>>

    @Query("DELETE FROM charge_sessions")
    suspend fun clear()
}
