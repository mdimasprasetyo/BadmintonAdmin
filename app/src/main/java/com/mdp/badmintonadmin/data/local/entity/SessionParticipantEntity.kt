package com.mdp.badmintonadmin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_participants")
data class SessionParticipantEntity(
    @PrimaryKey val playerId: Int, // Links straight to the permanent player ID
    val name: String,
    val activeTier: String, // Tracks mid-game temporary adjustments cleanly
    val gender: String = "M", // M, W
    val arrivalIndex: Int,
    val gamesPlayed: Int,
    val matchesSatOutConsecutively: Int,
    val isActivePresent: Boolean = true
)