package app.shuttlers.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.shuttlers.data.local.AppDatabase
import app.shuttlers.data.local.entity.PlayerEntity
import app.shuttlers.data.local.entity.SessionHistoryEntity
import app.shuttlers.data.local.entity.SessionParticipantEntity
import app.shuttlers.data.session.SessionStore
import app.shuttlers.domain.matchmaker.Matchmaker
import app.shuttlers.domain.matchmaker.PriorityEngine
import app.shuttlers.domain.model.CourtAssignment
import app.shuttlers.domain.model.PlayerMatchmakingProfile
import app.shuttlers.domain.session.MatchWinner
import app.shuttlers.domain.session.SessionRules
import app.shuttlers.domain.session.UndoStack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DashboardUiState(
    val isActiveSessionRunning: Boolean = false,
    val totalCourtsBooked: Int = 2,
    val sessionDurationMinutes: Int = 120, // Default 120
    val activeCourts: List<CourtAssignment> = emptyList(),
    val waitingLounge: List<PlayerMatchmakingProfile> = emptyList(),
    val displayWarnings: List<String> = emptyList(),
    val historyViewSession: SessionHistoryEntity? = null,
    /** Label of the action Undo would reverse ("Court 2 finished"), or null when there is nothing to undo. */
    val undoLabel: String? = null
)

/** Everything needed to put the board back the way it was before a finish, substitution or swap. */
internal data class UndoEntry(
    val label: String,
    val courts: List<CourtAssignment>,
    val participants: List<SessionParticipantEntity>
)

/** Accepted ranges for the session setup form. */
object SessionLimits {
    val COURTS = 1..20
    val DURATION_MINUTES = 20..720
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val playerDao = AppDatabase.getDatabase(application).playerDao()
    private val matchmaker = Matchmaker()
    private val priorityEngine = PriorityEngine()
    private val sessionStore = SessionStore(application)

    private val _uiState = MutableStateFlow(DashboardUiState())

    // Undo history for court actions. Kept in memory only: it is cleared when a session starts or ends.
    private val undoStack = UndoStack<UndoEntry>()

    // Court actions read the participants, change them and write them back; serialise them so two
    // quick taps (or an undo racing a finish) cannot interleave.
    private val courtActionLock = Mutex()
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Restore a session that was running when Android killed the app.
        val saved = sessionStore.load()
        if (saved != null) {
            _uiState.value = DashboardUiState(
                isActiveSessionRunning = true,
                totalCourtsBooked = saved.courtsBooked,
                sessionDurationMinutes = saved.durationMinutes
            )
        }
        viewModelScope.launch {
            if (saved != null) restoreCourts(saved)
            // From here on, every state change is mirrored to disk.
            _uiState.collect { persistSession(it) }
        }
    }

    private fun persistSession(state: DashboardUiState) {
        if (!state.isActiveSessionRunning) {
            sessionStore.clear()
            return
        }
        sessionStore.save(
            courtsBooked = state.totalCourtsBooked,
            durationMinutes = state.sessionDurationMinutes,
            courts = state.activeCourts.map {
                SessionStore.SavedCourt(
                    courtNumber = it.courtNumber,
                    team1 = it.team1.first.playerId to it.team1.second.playerId,
                    team2 = it.team2.first.playerId to it.team2.second.playerId
                )
            }
        )
    }

    private suspend fun restoreCourts(saved: SessionStore.SavedSession) {
        val profiles = playerDao.getActiveSessionParticipants().first()
            .map { it.toProfile() }
            .associateBy { it.playerId }
        val courts = saved.courts.mapNotNull { court ->
            val a = profiles[court.team1.first]
            val b = profiles[court.team1.second]
            val c = profiles[court.team2.first]
            val d = profiles[court.team2.second]
            if (a == null || b == null || c == null || d == null) null
            else CourtAssignment(court.courtNumber, a to b, c to d)
        }
        _uiState.value = _uiState.value.copy(activeCourts = courts)
        refreshMatchmakingLineups()
    }

    private fun SessionParticipantEntity.toProfile() = PlayerMatchmakingProfile(
        playerId = playerId,
        name = name,
        activeTier = activeTier,
        gender = gender,
        arrivalIndex = arrivalIndex,
        gamesPlayed = gamesPlayed,
        matchesSatOutConsecutively = matchesSatOutConsecutively,
        isActivePresent = isActivePresent,
        sessionWins = wins,
        sessionLosses = losses
    )

    // NEW: Observe the permanent roster from the database
    val permanentRoster: StateFlow<List<PlayerEntity>> = playerDao.getAllPermanentPlayers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // NEW: Observe session history
    val sessionHistory: StateFlow<List<SessionHistoryEntity>> = playerDao.getAllSessionHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startSession(courts: Int, durationMinutes: Int) {
        viewModelScope.launch {
            playerDao.clearCurrentSession()
            undoStack.clear()
            _uiState.value = DashboardUiState(
                isActiveSessionRunning = true,
                totalCourtsBooked = courts.coerceIn(SessionLimits.COURTS),
                sessionDurationMinutes = durationMinutes.coerceIn(SessionLimits.DURATION_MINUTES)
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
        undoStack.clear()
        _uiState.value = _uiState.value.copy(
            isActiveSessionRunning = false,
            activeCourts = emptyList(),
            waitingLounge = emptyList(),
            displayWarnings = emptyList(),
            undoLabel = null
        )
    }

    fun saveAndEndSession() {
        viewModelScope.launch {
            // Only matches that were marked finished count. Courts still in play when the
            // session ends are not credited (they may have only just started).
            val updatedParticipants = playerDao.getActiveSessionParticipants().first()
            val timestamp = System.currentTimeMillis()

            if (updatedParticipants.isNotEmpty()) {
                val dataString = updatedParticipants.joinToString("\n") {
                    SessionRules.encodeHistoryLine(it)
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
                            totalWins = player.totalWins + participant.wins,
                            totalLosses = player.totalLosses + participant.losses,
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

        val domainProfiles = currentParticipantsEntities.map { it.toProfile() }

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

    /**
     * Finishes the match on [courtAssignment]. [winner] credits a win to one pair and a loss to the
     * other for tonight; the totals reach the permanent roster on Save & End, like games played.
     */
    fun completeMatch(courtAssignment: CourtAssignment, winner: MatchWinner = MatchWinner.NONE) {
        viewModelScope.launch {
            courtActionLock.withLock {
                // Get current participants again to catch any mid-game substitutions
                val currentEntities = playerDao.getActiveSessionParticipants().first()
                val court = _uiState.value.activeCourts.find { it.stableId == courtAssignment.stableId }
                    ?: return@withLock
                pushUndo("Court ${court.courtNumber} finished", currentEntities)

                // Credit the players currently on the court (not a stale copy from the UI)
                val updatedEntities = SessionRules.creditFinishedMatch(currentEntities, court, winner)
                playerDao.insertSessionParticipants(updatedEntities)

                // Remove finished court from activeCourts before refreshing
                val updatedCourts = _uiState.value.activeCourts.filter { it.stableId != court.stableId }
                _uiState.value = _uiState.value.copy(activeCourts = updatedCourts)

                refreshMatchmakingLineups()
            }
        }
    }

    /** Reverses the most recent finish, substitution or swap. */
    fun undoLastAction() {
        viewModelScope.launch {
            courtActionLock.withLock {
                val entry = undoStack.pop() ?: return@withLock
                if (!_uiState.value.isActiveSessionRunning) return@withLock

                val currentEntities = playerDao.getActiveSessionParticipants().first()
                playerDao.insertSessionParticipants(SessionRules.restoreStats(currentEntities, entry.participants))

                // Courts go back exactly as they were; refreshing re-syncs names/tiers from the
                // database and fills any court that is free.
                _uiState.value = _uiState.value.copy(
                    activeCourts = entry.courts,
                    undoLabel = undoStack.peek()?.label
                )
                refreshMatchmakingLineups()
            }
        }
    }

    private fun pushUndo(label: String, participants: List<SessionParticipantEntity>) {
        undoStack.push(UndoEntry(label, _uiState.value.activeCourts, participants))
        _uiState.value = _uiState.value.copy(undoLabel = label)
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
                
                viewModelScope.launch {
                    courtActionLock.withLock {
                        pushUndo("Players swapped on Court ${court.courtNumber}", playerDao.getActiveSessionParticipants().first())
                        val latest = _uiState.value.activeCourts.toMutableList()
                        val latestIndex = latest.indexOfFirst { it.stableId == courtId }
                        if (latestIndex != -1) {
                            latest[latestIndex] = court.copy(team1 = newTeam1, team2 = newTeam2)
                            _uiState.value = _uiState.value.copy(activeCourts = latest)
                        }
                    }
                }
            }
        }
    }

    fun substitutePlayer(court: CourtAssignment, oldPlayerId: Int, newPlayerProfile: PlayerMatchmakingProfile) {
        viewModelScope.launch {
            courtActionLock.withLock {
                val currentCourts = _uiState.value.activeCourts.toMutableList()
                val index = currentCourts.indexOfFirst { it.stableId == court.stableId }
                if (index == -1) return@withLock

                // 1. Credit the injured/replaced player IMMEDIATELY for the game they started
                val currentEntities = playerDao.getActiveSessionParticipants().first()
                val outgoingName = currentEntities.find { it.playerId == oldPlayerId }?.name ?: "Player"
                pushUndo("$outgoingName substituted on Court ${court.courtNumber}", currentEntities)

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
