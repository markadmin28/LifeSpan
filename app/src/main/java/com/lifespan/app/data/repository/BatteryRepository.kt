package com.lifespan.app.data.repository

import com.lifespan.app.data.db.ChargeSessionDao
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogDao
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.session.SessionAccumulator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Single source of truth for live battery state and persisted charge-session
 * history. The foreground service feeds snapshots in via [record]; the UI
 * observes [latest], [monitoring] and [observeRecentSessions].
 */
class BatteryRepository(
    private val sessionDao: ChargeSessionDao,
    private val telemetryDao: TelemetryLogDao,
) {
    companion object {
        /** How long raw telemetry samples are kept before being pruned. */
        const val TELEMETRY_RETENTION_DAYS = 14
        private const val TELEMETRY_RETENTION_MILLIS =
            TELEMETRY_RETENTION_DAYS * 24L * 60L * 60L * 1000L
    }

    private val _latest = MutableStateFlow<BatterySnapshot?>(null)
    val latest: StateFlow<BatterySnapshot?> = _latest.asStateFlow()

    private val _monitoring = MutableStateFlow(false)
    val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    private val mutex = Mutex()
    private var accumulator: SessionAccumulator? = null
    private var activeSessionId: Long? = null

    fun setMonitoring(active: Boolean) {
        _monitoring.value = active
    }

    fun observeRecentSessions(limit: Int = 50): Flow<List<ChargeSessionEntity>> =
        sessionDao.observeRecent(limit)

    fun observeSessionLogs(sessionId: Long): Flow<List<TelemetryLogEntity>> =
        telemetryDao.observeForSession(sessionId)

    fun observeRecentTelemetry(limit: Int = 1_000): Flow<List<TelemetryLogEntity>> =
        telemetryDao.observeRecent(limit)

    /** Delete telemetry samples older than the retention window. */
    suspend fun pruneOldTelemetry(now: Long = System.currentTimeMillis()) {
        telemetryDao.deleteOlderThan(now - TELEMETRY_RETENTION_MILLIS)
    }

    /** Erase all recorded sessions and telemetry (the active session included). */
    suspend fun clearHistory() = mutex.withLock {
        sessionDao.clear()
        telemetryDao.clearAll()
        accumulator = null
        activeSessionId = null
    }

    /** Adopt an already-open session (e.g. after a service restart). */
    suspend fun restoreActiveSession() = mutex.withLock {
        if (activeSessionId != null) return@withLock
        val open = sessionDao.getActiveSession() ?: return@withLock
        activeSessionId = open.id
        accumulator = SessionAccumulator(open.startTime, open.startLevel, open.plugType)
    }

    /**
     * Record a new [snapshot]: publish it as the latest reading, persist a
     * telemetry row, and open/append/close the current charge session based on
     * the charging state.
     */
    suspend fun record(snapshot: BatterySnapshot) = mutex.withLock {
        _latest.value = snapshot
        when {
            snapshot.isCharging -> {
                if (activeSessionId == null) startSession(snapshot)
                accumulator?.add(snapshot)
                persistTelemetry(snapshot, activeSessionId)
                updateSessionRow(snapshot, closed = false)
            }

            activeSessionId != null -> {
                accumulator?.add(snapshot)
                persistTelemetry(snapshot, activeSessionId)
                updateSessionRow(snapshot, closed = true)
                accumulator = null
                activeSessionId = null
            }

            else -> persistTelemetry(snapshot, null)
        }
    }

    private suspend fun startSession(snapshot: BatterySnapshot) {
        accumulator = SessionAccumulator(snapshot.timestamp, snapshot.level, snapshot.plugType.label)
        activeSessionId = sessionDao.insert(
            ChargeSessionEntity(
                startTime = snapshot.timestamp,
                startLevel = snapshot.level,
                plugType = snapshot.plugType.label,
                peakTempCelsius = snapshot.temperatureCelsius,
                peakCurrentMa = abs(snapshot.currentMa),
            ),
        )
    }

    private suspend fun updateSessionRow(snapshot: BatterySnapshot, closed: Boolean) {
        val id = activeSessionId ?: return
        val acc = accumulator ?: return
        sessionDao.update(
            ChargeSessionEntity(
                id = id,
                startTime = acc.startTime,
                endTime = if (closed) snapshot.timestamp else null,
                startLevel = acc.startLevel,
                endLevel = snapshot.level,
                plugType = acc.plugType,
                peakTempCelsius = acc.peakTempOrZero(),
                peakCurrentMa = acc.peakCurrentMa,
                totalMahAdded = acc.totalMahAdded,
            ),
        )
    }

    private suspend fun persistTelemetry(snapshot: BatterySnapshot, sessionId: Long?) {
        telemetryDao.insert(
            TelemetryLogEntity(
                sessionId = sessionId,
                timestamp = snapshot.timestamp,
                batteryLevel = snapshot.level,
                voltageMv = snapshot.voltageMv,
                currentMa = snapshot.currentMa.roundToInt(),
                temperatureCelsius = snapshot.temperatureCelsius,
                isCharging = snapshot.isCharging,
            ),
        )
    }
}
