package app.shuttlers.domain.matchmaker

import app.shuttlers.domain.model.CourtAssignment
import app.shuttlers.domain.model.PlayerMatchmakingProfile

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

            // Every candidate court includes the highest-priority player (index 0), who must play next.
            // Pick, in order of preference:
            //   1. a skill-balanced same-gender court (closest to the front of the queue),
            //   2. a skill-balanced mixed court, preferring 2M+2W so teams can be MW vs MW,
            //      then queue order,
            //   3. if nothing passes the balance rule, the court with the smallest skill gap
            //      (instead of blindly taking the next four in the queue).
            val anchor = sortedWaitingList[0]
            var bestSameGender: Candidate? = null
            var bestMixed: Candidate? = null
            var bestAny: Candidate? = null

            for (j in 1 until sortedWaitingList.size - 2) {
                for (k in j + 1 until sortedWaitingList.size - 1) {
                    for (l in k + 1 until sortedWaitingList.size) {
                        val players = listOf(anchor, sortedWaitingList[j], sortedWaitingList[k], sortedWaitingList[l])
                        val gap = skillGap(players)
                        val queueCost = j + k + l // lower = closer to the front of the queue
                        val womenCount = players.count { it.gender == "W" }

                        val candidate = Candidate(players, gap, queueCost)
                        if (bestAny == null || candidate.smallerGapThan(bestAny)) bestAny = candidate

                        if (!isCourtSkillBalanced(players[0], players[1], players[2], players[3])) continue

                        if (womenCount == 0 || womenCount == 4) {
                            if (bestSameGender == null || candidate.earlierInQueueThan(bestSameGender)) bestSameGender = candidate
                        } else {
                            // Uneven gender split (3+1) cannot form MW vs MW, so rank it below 2+2.
                            val mixedCandidate = candidate.copy(unevenGender = womenCount != 2)
                            if (bestMixed == null || mixedCandidate.earlierInQueueThan(bestMixed)) bestMixed = mixedCandidate
                        }
                    }
                }
            }

            val chosen = (bestSameGender ?: bestMixed ?: bestAny)?.players ?: break
            assignments.add(createBalancedCourtAssignment(courtNum, chosen))
            chosen.forEach { sortedWaitingList.remove(it) }
        }

        return assignments
    }

    private data class Candidate(
        val players: List<PlayerMatchmakingProfile>,
        val gap: Double,
        val queueCost: Int,
        val unevenGender: Boolean = false
    ) {
        /** Fairness first: who has waited longest plays next; skill gap breaks ties. */
        fun earlierInQueueThan(other: Candidate): Boolean = when {
            unevenGender != other.unevenGender -> !unevenGender
            queueCost != other.queueCost -> queueCost < other.queueCost
            else -> gap < other.gap
        }

        /** Fallback when no court passes the balance rule: least lopsided court wins. */
        fun smallerGapThan(other: Candidate): Boolean =
            gap < other.gap || (gap == other.gap && queueCost < other.queueCost)
    }

    private fun skillGap(players: List<PlayerMatchmakingProfile>): Double {
        val weights = players.map { getTierWeight(it.activeTier) }
        return weights.max() - weights.min()
    }

    /**
     * Splits 4 players into two teams. Of the three possible pairings it picks the one with the
     * smallest difference in combined tier weight; when the court is 2 men + 2 women, pairings that
     * give each team one man and one woman (MW vs MW) always win over MM vs WW.
     */
    internal fun createBalancedCourtAssignment(courtNum: Int, players: List<PlayerMatchmakingProfile>): CourtAssignment {
        val (a, b, c, d) = players
        val pairings = listOf(
            (a to b) to (c to d),
            (a to c) to (b to d),
            (a to d) to (b to c)
        )
        val twoAndTwo = players.count { it.gender == "W" } == 2

        fun teamWeight(team: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>) =
            getTierWeight(team.first.activeTier) + getTierWeight(team.second.activeTier)

        fun isMixedPair(team: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>) =
            team.first.gender != team.second.gender

        val best = pairings.minWith(
            compareBy<Pair<Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>, Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>>>(
                { (t1, t2) -> if (twoAndTwo && !(isMixedPair(t1) && isMixedPair(t2))) 1 else 0 },
                { (t1, t2) -> kotlin.math.abs(teamWeight(t1) - teamWeight(t2)) }
            )
        )
        return CourtAssignment(courtNum, best.first, best.second)
    }
}
