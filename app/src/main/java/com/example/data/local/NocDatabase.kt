package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OltEntity::class,
        PonPortEntity::class,
        OnuEntity::class,
        OnuPowerHistoryEntity::class,
        MikrotikEntity::class,
        NetworkInterfaceEntity::class,
        AlertEntity::class,
        ThresholdSettingsEntity::class,
        IspPortalEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NocDatabase : RoomDatabase() {

    abstract fun nocDao(): NocDao

    companion object {
        @Volatile
        private var INSTANCE: NocDatabase? = null
        @Volatile
        private var lastAppContext: Context? = null

        fun getDatabase(context: Context): NocDatabase {
            val appCtx = context.applicationContext
            val existing = INSTANCE
            if (existing != null && lastAppContext === appCtx && existing.isOpen) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && lastAppContext === appCtx && current.isOpen) {
                    current
                } else {
                    val instance = Room.databaseBuilder(
                        appCtx,
                        NocDatabase::class.java,
                        "netpulse_noc.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    lastAppContext = appCtx
                    instance
                }
            }
        }
    }
}
