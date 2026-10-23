package com.mdp.badmintonadmin.data.local.entity

import androidx.room.ColumnInfo
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
    val isActivePresent: Boolean = true,
    // Tonight's results (added in DB version 5). Credited to the permanent record on Save & End.
    @ColumnInfo(defaultValue = "0") val wins: Int = 0,
    @ColumnInfo(defaultValue = "0") val losses: Int = 0
)
