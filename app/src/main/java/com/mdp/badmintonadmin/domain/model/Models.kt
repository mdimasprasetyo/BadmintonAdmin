package com.mdp.badmintonadmin.domain.model

data class PlayerMatchmakingProfile(
    val playerId: Int,
    val name: String,
    val activeTier: String, // S, A, B, C, D, X (handles temporary mid-game adjustments)
    val gender: String, // M, W
    val arrivalIndex: Int,
    val gamesPlayed: Int,
    val matchesSatOutConsecutively: Int,
    val isActivePresent: Boolean = true
)

data class CourtAssignment(
    val courtNumber: Int,
    val team1: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>,
    val team2: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>
) {
    val stableId: Long get() = courtNumber.toLong() // Help Compose track item movement by court slot

    // Helper to get all 4 player IDs assigned to this court
    fun getAllPlayerIds(): List<Int> = listOf(
        team1.first.playerId, team1.second.playerId,
        team2.first.playerId, team2.second.playerId
    )
}