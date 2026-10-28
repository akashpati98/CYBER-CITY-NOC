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

        fun getDatabase(context: Context): NocDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NocDatabase::class.java,
                    "netpulse_noc.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
