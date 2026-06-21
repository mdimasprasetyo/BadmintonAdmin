package com.mdp.badmintonadmin.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mdp.badmintonadmin.data.local.AppDatabase
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.data.local.entity.SessionParticipantEntity
import com.mdp.badmintonadmin.domain.matchmaker.Matchmaker
import com.mdp.badmintonadmin.domain.matchmaker.PriorityEngine
import com.mdp.badmintonadmin.domain.model.CourtAssignment
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isActiveSessionRunning: Boolean = false,
    val totalCourtsBooked: Int = 2,
    val sessionDurationMinutes: Int = 120, // Default 120
    val activeCourts: List<CourtAssignment> = emptyList(),
    val waitingLounge: List<PlayerMatchmakingProfile> = emptyList(),
    val displayWarnings: List<String> = emptyList(),
    val historyViewSession: SessionHistoryEntity? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val playerDao = AppDatabase.getDatabase(application).playerDao()
    private val matchmaker = Matchmaker()
    private val priorityEngine = PriorityEngine()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // NEW: Observe the permanent roster from the database
    val permanentRoster: StateFlow<List<PlayerEntity>> = playerDao.getAllPermanentPlayers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // NEW: Observe session history
    val sessionHistory: StateFlow<List<SessionHistoryEntity>> = playerDao.getAllSessionHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startSession(courts: Int, durationMinutes: Int) {
        viewModelScope.launch {
            playerDao.clearCurrentSession()
            _uiState.value = DashboardUiState(
                isActiveSessionRunning = true,
                totalCourtsBooked = courts,
                sessionDurationMinutes = durationMinutes
            )
            refreshMatchmakingLineups()
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            performReset()
        }
    }

    private suspend fun performReset() {
        playerDao.clearCurrentSession()
        _uiState.value = _uiState.value.copy(
            isActiveSessionRunning = false,
            activeCourts = emptyList(),
            waitingLounge = emptyList(),
            displayWarnings = emptyList()
        )
    }

    fun saveAndEndSession() {
        viewModelScope.launch {
            // Auto-finish matches: Increment gamesPlayed for everyone on court
            val playingIds = _uiState.value.activeCourts.flatMap { it.getAllPlayerIds() }.toSet()
            val currentParticipants = playerDao.getActiveSessionParticipants().first()
            val timestamp = System.currentTimeMillis()
            
            val updatedParticipants = currentParticipants.map { participant ->
                if (participant.playerId in playingIds) {
                    participant.copy(
                        gamesPlayed = participant.gamesPlayed + 1,
                        matchesSatOutConsecutively = 0
                    )
                } else {
                    participant
                }
            }

            if (updatedParticipants.isNotEmpty()) {
                val dataString = updatedParticipants.joinToString("\n") { 
                    "${it.name}|${it.gamesPlayed}|${it.activeTier}" 
                }
                
                val history = SessionHistoryEntity(
                    durationMinutes = _uiState.value.sessionDurationMinutes,
                    courtsBooked = _uiState.value.totalCourtsBooked,
                    participantDataJson = dataString
                )
                playerDao.saveSessionHistory(history)

                // Update permanent player statistics
                val roster = playerDao.getAllPermanentPlayers().first()
                updatedParticipants.forEach { participant ->
                    roster.find { it.id == participant.playerId }?.let { player ->
                        val updatedPlayer = player.copy(
                            totalMatchesPlayed = player.totalMatchesPlayed + participant.gamesPlayed,
                            totalSessionsParticipated = player.totalSessionsParticipated + 1,
                            lastPlayedTimestamp = timestamp
                        )
                        playerDao.updatePlayer(updatedPlayer)
                    }
                }
            }
            performReset()
        }
    }

    fun viewHistorySession(session: SessionHistoryEntity?) {
        _uiState.value = _uiState.value.copy(historyViewSession = session)
    }

    fun deleteSessionHistory(session: SessionHistoryEntity) {
        viewModelScope.launch {
            playerDao.deleteSessionHistory(session)
        }
    }

    fun clearAllSessionHistory() {
        viewModelScope.launch {
            playerDao.clearAllSessionHistory()
        }
    }

    // NEW: Register a brand new player to the club AND check them in tonight
    fun registerNewPlayerAndCheckIn(name: String, baseTier: String, gender: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val newPlayer = PlayerEntity(name = trimmedName, baseTier = baseTier.uppercase(), gender = gender.uppercase())
            val generatedId = playerDao.insertPlayer(newPlayer).toInt()
            checkInPlayer(generatedId, trimmedName, baseTier.uppercase(), gender.uppercase())
        }
    }

    // NEW: Check an existing player into tonight's session
    fun checkInPlayer(playerId: Int, name: String, baseTier: String, gender: String) {
        viewModelScope.launch {
            val currentSession = playerDao.getActiveSessionParticipants().first()

            // Prevent accidental double check-ins
            if (currentSession.any { it.playerId == playerId }) return@launch

            // Give them the next available arrival index
            val nextArrivalIndex = (currentSession.maxOfOrNull { it.arrivalIndex } ?: 0) + 1

            val newParticipant = SessionParticipantEntity(
                playerId = playerId,
                name = name,
                activeTier = baseTier,
                gender = gender,
                arrivalIndex = nextArrivalIndex,
                gamesPlayed = 0,
                matchesSatOutConsecutively = 0,
                isActivePresent = true
            )

            val updatedList = currentSession + newParticipant
            playerDao.insertSessionParticipants(updatedList)
            refreshMatchmakingLineups()
        }
    }

    private suspend fun refreshMatchmakingLineups() {
        val currentParticipantsEntities = playerDao.getActiveSessionParticipants().first()
        if (currentParticipantsEntities.isEmpty()) return

        val domainProfiles = currentParticipantsEntities.map {
            PlayerMatchmakingProfile(
                playerId = it.playerId,
                name = it.name,
                activeTier = it.activeTier,
                gender = it.gender,
                arrivalIndex = it.arrivalIndex,
                gamesPlayed = it.gamesPlayed,
                matchesSatOutConsecutively = it.matchesSatOutConsecutively,
                isActivePresent = it.isActivePresent
            )
        }

        val globalMin = domainProfiles.filter { it.isActivePresent }.minOfOrNull { it.gamesPlayed } ?: 0

        // Keep existing courts if they are still valid (players still present and active)
        val existingCourts = _uiState.value.activeCourts.filter { court ->
            court.getAllPlayerIds().all { id -> 
                domainProfiles.find { it.playerId == id }?.isActivePresent == true 
            }
        }.map { court ->
            // UPDATE: Ensure names/tiers in active courts are synced with DB updates
            fun updateProfile(p: PlayerMatchmakingProfile): PlayerMatchmakingProfile {
                return domainProfiles.find { it.playerId == p.playerId } ?: p
            }
            court.copy(
                team1 = updateProfile(court.team1.first) to updateProfile(court.team1.second),
                team2 = updateProfile(court.team2.first) to updateProfile(court.team2.second)
            )
        }
        val occupiedPlayerIds = existingCourts.flatMap { it.getAllPlayerIds() }.toSet()
        
        // Filter out players already on court from the pool for new assignments
        val availablePool = domainProfiles.filter { it.playerId !in occupiedPlayerIds && it.isActivePresent }

        val newAssignments = matchmaker.autoGenerateLineups(
            allParticipants = availablePool,
            courtsBooked = _uiState.value.totalCourtsBooked - existingCourts.size,
            globalMinGames = globalMin
        )

        // Map the new assignments to the next available court numbers
        val usedCourtNumbers = existingCourts.map { it.courtNumber }.toSet()
        var nextCourtNum = 1
        val finalNewAssignments = newAssignments.map { 
            while (usedCourtNumbers.contains(nextCourtNum)) nextCourtNum++
            it.copy(courtNumber = nextCourtNum++)
        }

        val allAssignedCourts = (existingCourts + finalNewAssignments).sortedBy { it.courtNumber }
        val allAssignedPlayerIds = allAssignedCourts.flatMap { it.getAllPlayerIds() }.toSet()
        val restingLounge = domainProfiles.filter { it.playerId !in allAssignedPlayerIds }

        _uiState.value = _uiState.value.copy(
            activeCourts = allAssignedCourts,
            waitingLounge = restingLounge.sortedByDescending {
                priorityEngine.calculatePriorityScore(it.arrivalIndex, it.gamesPlayed, it.matchesSatOutConsecutively, globalMin)
            }
        )
    }

    fun completeMatch(courtAssignment: CourtAssignment) {
        viewModelScope.launch {
            // Get current participants again to catch any mid-game substitutions
            val currentEntities = playerDao.getActiveSessionParticipants().first()
            
            // We use the player IDs currently on the court assignment
            val playingIds = courtAssignment.getAllPlayerIds().toSet()

            val updatedEntities = currentEntities.map { participant ->
                // Check if player was part of the FINISHING match
                if (participant.playerId in playingIds) {
                    participant.copy(
                        gamesPlayed = participant.gamesPlayed + 1,
                        matchesSatOutConsecutively = 0
                    )
                } else if (participant.isActivePresent) {
                    participant.copy(
                        matchesSatOutConsecutively = participant.matchesSatOutConsecutively + 1
                    )
                } else {
                    participant
                }
            }

            playerDao.insertSessionParticipants(updatedEntities)
            
            // FIX: Remove finished court from activeCourts before refreshing
            val updatedCourts = _uiState.value.activeCourts.filter { it.stableId != courtAssignment.stableId }
            _uiState.value = _uiState.value.copy(activeCourts = updatedCourts)

            refreshMatchmakingLineups()
        }
    }

    fun modifyTierMidGame(playerId: Int, temporaryNewTier: String) {
        viewModelScope.launch {
            val currentEntities = playerDao.getActiveSessionParticipants().first()
            val targetedPlayer = currentEntities.find { it.playerId == playerId }

            targetedPlayer?.let {
                val updatedPlayer = it.copy(activeTier = temporaryNewTier.uppercase())
                playerDao.updateSessionParticipant(updatedPlayer)
                refreshMatchmakingLineups()
            }
        }
    }

    fun updatePlayerInfo(playerId: Int, newName: String, newGender: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            // 1. Update permanent roster
            val roster = permanentRoster.value
            roster.find { it.id == playerId }?.let {
                playerDao.updatePlayer(it.copy(name = trimmedName, gender = newGender.uppercase()))
            }

            // 2. Update current session participant
            val currentParticipants = playerDao.getActiveSessionParticipants().first()
            currentParticipants.find { it.playerId == playerId }?.let {
                playerDao.updateSessionParticipant(it.copy(name = trimmedName, gender = newGender.uppercase()))
            }

            // 3. Refresh UI
            refreshMatchmakingLineups()
        }
    }

    fun togglePlayerPresence(playerId: Int) {
        viewModelScope.launch {
            val currentEntities = playerDao.getActiveSessionParticipants().first()
            val targetedPlayer = currentEntities.find { it.playerId == playerId }

            targetedPlayer?.let {
                val updatedPlayer = it.copy(isActivePresent = !it.isActivePresent)
                playerDao.updateSessionParticipant(updatedPlayer)
                refreshMatchmakingLineups()
            }
        }
    }

    fun swapPlayersWithinCourt(courtId: Long, playerId1: Int, playerId2: Int) {
        val currentCourts = _uiState.value.activeCourts.toMutableList()
        val index = currentCourts.indexOfFirst { it.stableId == courtId }
        if (index != -1) {
            val court = currentCourts[index]
            val allPlayers = listOf(court.team1.first, court.team1.second, court.team2.first, court.team2.second)
            
            val p1 = allPlayers.find { it.playerId == playerId1 }
            val p2 = allPlayers.find { it.playerId == playerId2 }
            
            if (p1 != null && p2 != null) {
                fun replace(p: PlayerMatchmakingProfile): PlayerMatchmakingProfile {
                    return when (p.playerId) {
                        playerId1 -> p2
                        playerId2 -> p1
                        else -> p
                    }
                }
                
                val newTeam1 = replace(court.team1.first) to replace(court.team1.second)
                val newTeam2 = replace(court.team2.first) to replace(court.team2.second)
                
                currentCourts[index] = court.copy(team1 = newTeam1, team2 = newTeam2)
                _uiState.value = _uiState.value.copy(activeCourts = currentCourts)
            }
        }
    }

    fun substitutePlayer(court: CourtAssignment, oldPlayerId: Int, newPlayerProfile: PlayerMatchmakingProfile) {
        viewModelScope.launch {
            // 1. Credit the injured/replaced player IMMEDIATELY for the game they started
            val currentEntities = playerDao.getActiveSessionParticipants().first()
            val updatedEntities = currentEntities.map { participant ->
                if (participant.playerId == oldPlayerId) {
                    participant.copy(
                        gamesPlayed = participant.gamesPlayed + 1,
                        matchesSatOutConsecutively = 0,
                        isActivePresent = false // Mark as absent (injured/home) by default on substitution
                    )
                } else {
                    participant
                }
            }
            playerDao.insertSessionParticipants(updatedEntities)

            // 2. Update the UI state for the court assignment
            val currentCourts = _uiState.value.activeCourts.toMutableList()
            val index = currentCourts.indexOfFirst { it.stableId == court.stableId }
            if (index != -1) {
                val oldCourt = currentCourts[index]
                
                fun replaceInTeam(team: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>): Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile> {
                    return when {
                        team.first.playerId == oldPlayerId -> team.copy(first = newPlayerProfile)
                        team.second.playerId == oldPlayerId -> team.copy(second = newPlayerProfile)
                        else -> team
                    }
                }

                currentCourts[index] = oldCourt.copy(
                    team1 = replaceInTeam(oldCourt.team1),
                    team2 = replaceInTeam(oldCourt.team2)
                )
                
                _uiState.value = _uiState.value.copy(activeCourts = currentCourts)
                refreshMatchmakingLineups()
            }
        }
    }
}