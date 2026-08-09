package com.lifespan.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChargeSessionEntity::class, TelemetryLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LifeSpanDatabase : RoomDatabase() {

    abstract fun chargeSessionDao(): ChargeSessionDao
    abstract fun telemetryLogDao(): TelemetryLogDao

    companion object {
        private const val DB_NAME = "lifespan.db"

        @Volatile
        private var instance: LifeSpanDatabase? = null

        fun getInstance(context: Context): LifeSpanDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): LifeSpanDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LifeSpanDatabase::class.java,
                DB_NAME,
            ).fallbackToDestructiveMigration().build()
    }
}
