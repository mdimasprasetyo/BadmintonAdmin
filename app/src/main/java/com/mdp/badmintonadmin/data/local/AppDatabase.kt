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
    exportSchema = false
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
                    .fallbackToDestructiveMigration() // Graceful fallback if database structures change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}