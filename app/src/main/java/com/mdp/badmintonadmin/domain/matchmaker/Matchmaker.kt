package com.mdp.badmintonadmin.domain.matchmaker

import com.mdp.badmintonadmin.domain.model.CourtAssignment
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import kotlin.math.abs

class Matchmaker(private val priorityEngine: PriorityEngine = PriorityEngine()) {

    /**
     * Converts alphabetical tiers into numeric weights for mathematical evaluation.
     */
    private fun getTierWeight(tier: String): Double {
        return when (tier.uppercase()) {
            "S" -> 5.0
            "A" -> 4.0
            "B" -> 3.0
            "C" -> 2.0
            "D" -> 1.0
            "X" -> 3.5 // Unknown/wildcard level sits comfortably between A and B
            else -> 3.0 // Default fallback to intermediate
        }
    }

    /**
     * Checks if a 4-player court combination adheres to the strict skill gap matrix.
     * Incorporates our Constraint Relaxation (Safety Valve) rule if players are starving on the bench.
     */
    private fun isCourtSkillBalanced(
        p1: PlayerMatchmakingProfile,
        p2: PlayerMatchmakingProfile,
        p3: PlayerMatchmakingProfile,
        p4: PlayerMatchmakingProfile
    ): Boolean {
        val weights = listOf(p1, p2, p3, p4).map { getTierWeight(it.activeTier) }
        val maxWeight = weights.maxOrNull() ?: 5.0
        val minWeight = weights.minOrNull() ?: 1.0
        val skillGap = maxWeight - minWeight

        // HARD CEILING / SOFT CONSTRAINT RULE:
        // Default max skill gap variance is 2.0 (e.g., S can play down to B, but not C or D).
        // If anyone on the court has sat out 3+ matches consecutively, relax the rules to 3.0.
        val maxAllowedGap = if (
            p1.matchesSatOutConsecutively >= 3 ||
            p2.matchesSatOutConsecutively >= 3 ||
            p3.matchesSatOutConsecutively >= 3 ||
            p4.matchesSatOutConsecutively >= 3
        ) {
            3.0 // Allows S to play with C, or A to play with D to break queue blocks
        } else {
            2.0 // Strict standard matching
        }

        return skillGap <= maxAllowedGap
    }

    /**
     * Main pipeline method: Evaluates the active crowd, sorts them by priority score,
     * and assembles balanced 4-player lineups for available courts.
     */
    fun autoGenerateLineups(
        allParticipants: List<PlayerMatchmakingProfile>,
        courtsBooked: Int,
        globalMinGames: Int
    ): List<CourtAssignment> {
        // Filter out anyone marked absent or injured mid-session
        val activePool = allParticipants.filter { it.isActivePresent }
        if (activePool.size < 4) return emptyList()

        // Calculate and sort the entire pool by their current Priority Score
        val sortedWaitingList = activePool.map { player ->
            val score = priorityEngine.calculatePriorityScore(
                arrivalIndex = player.arrivalIndex,
                gamesPlayed = player.gamesPlayed,
                matchesSatOutConsecutively = player.matchesSatOutConsecutively,
                globalMinGames = globalMinGames
            )
            player to score
        }.sortedByDescending { it.second }.map { it.first }.toMutableList()

        val assignments = mutableListOf<CourtAssignment>()
        val maxAvailableCourts = minOf(courtsBooked, activePool.size / 4)

        // Loop through each court slot and attempt to find a valid 4-player grouping
        for (courtNum in 1..maxAvailableCourts) {
            if (sortedWaitingList.size < 4) break

            var bestCombination: List<PlayerMatchmakingProfile>? = null
            var bestIsSameGender = false

            // Core Matchmaking Search: Evaluate high-priority candidates down the line
            // We search for the best combination containing the highest priority player (index 0)
            val i = 0
            for (j in i + 1 until sortedWaitingList.size - 2) {
                for (k in j + 1 until sortedWaitingList.size - 1) {
                    for (l in k + 1 until sortedWaitingList.size) {

                        val p1 = sortedWaitingList[i]
                        val p2 = sortedWaitingList[j]
                        val p3 = sortedWaitingList[k]
                        val p4 = sortedWaitingList[l]

                        if (isCourtSkillBalanced(p1, p2, p3, p4)) {
                            val players = listOf(p1, p2, p3, p4)
                            val isSameGender = players.all { it.gender == "M" } || players.all { it.gender == "W" }
                            
                            if (isSameGender) {
                                // Found a perfect skill-balanced same-gender match with top priority player
                                bestCombination = players
                                bestIsSameGender = true
                                break
                            } else if (bestCombination == null) {
                                // Keep the first balanced mixed match as fallback
                                bestCombination = players
                            }
                        }
                    }
                    if (bestIsSameGender) break
                }
                if (bestIsSameGender) break
            }

            if (bestCombination != null) {
                val assignedMatch = createBalancedCourtAssignment(courtNum, bestCombination)
                assignments.add(assignedMatch)
                bestCombination.forEach { sortedWaitingList.remove(it) }
            } else if (sortedWaitingList.size >= 4) {
                // Fallback: If no valid match passes the balance matrix due to extreme tier differences,
                // or if we couldn't even find a mixed match with the top player,
                // grab the top 4 remaining high-priority players and balance THEM manually.
                val fallbackPlayers = listOf(
                    sortedWaitingList.removeAt(0),
                    sortedWaitingList.removeAt(0),
                    sortedWaitingList.removeAt(0),
                    sortedWaitingList.removeAt(0)
                )
                val assignedMatch = createBalancedCourtAssignment(courtNum, fallbackPlayers)
                assignments.add(assignedMatch)
            }
        }

        return assignments
    }

    /**
     * Helper to group 4 players into two teams using the (1st+4th) vs (2nd+3rd) weight balancing pattern.
     */
    private fun createBalancedCourtAssignment(courtNum: Int, players: List<PlayerMatchmakingProfile>): CourtAssignment {
        val sortedByWeight = players.sortedByDescending { getTierWeight(it.activeTier) }
        
        // Best competitive balance pattern: (1st + 4th heaviest) vs (2nd + 3rd heaviest)
        val team1 = Pair(sortedByWeight[0], sortedByWeight[3])
        val team2 = Pair(sortedByWeight[1], sortedByWeight[2])
        
        return CourtAssignment(courtNum, team1, team2)
    }
}
