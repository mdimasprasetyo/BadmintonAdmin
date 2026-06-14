package com.mdp.badmintonadmin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int,
    val courtsBooked: Int,
    val participantDataJson: String // Serialized list of participants and their final stats
)
