package com.mdp.badmintonadmin.data.local.dao

import androidx.room.*
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.data.local.entity.SessionParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    // --- Permanent Roster Queries ---
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPermanentPlayers(): Flow<List<PlayerEntity>>

    // UPDATED: Now returns Long (the newly generated ID)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    // --- Active Session Queries ---
    @Query("SELECT * FROM session_participants ORDER BY arrivalIndex ASC")
    fun getActiveSessionParticipants(): Flow<List<SessionParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionParticipants(participants: List<SessionParticipantEntity>)

    @Update
    suspend fun updateSessionParticipant(participant: SessionParticipantEntity)

    @Query("DELETE FROM session_participants")
    suspend fun clearCurrentSession()

    // --- Session History Queries ---
    @Insert
    suspend fun saveSessionHistory(history: SessionHistoryEntity)

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun getAllSessionHistory(): Flow<List<SessionHistoryEntity>>
}