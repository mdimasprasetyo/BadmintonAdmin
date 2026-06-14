package com.mdp.badmintonadmin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val baseTier: String, // S, A, B, C, D, X (Permanent level profile)
    val gender: String = "M", // M, W
    val totalMatchesPlayed: Int = 0,
    val totalSessionsParticipated: Int = 0,
    val lastPlayedTimestamp: Long? = null,
    val dateAddedTimestamp: Long = System.currentTimeMillis(),
    val totalWins: Int = 0,
    val totalLosses: Int = 0
)