package com.mdp.badmintonadmin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mdp.badmintonadmin.data.local.dao.PlayerDao
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.data.local.entity.SessionParticipantEntity

@Database(
    entities = [PlayerEntity::class, SessionParticipantEntity::class, SessionHistoryEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "badminton_admin_db"
                )
                    // No destructive fallback on upgrade: a schema change must ship a Migration,
                    // otherwise the whole club roster would be wiped. Schemas are exported to app/schemas.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}