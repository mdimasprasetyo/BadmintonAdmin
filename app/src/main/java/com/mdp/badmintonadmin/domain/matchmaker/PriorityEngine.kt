package com.mdp.badmintonadmin.domain.matchmaker

import kotlin.math.ceil
import kotlin.math.floor

// Holds configuration settings for the active evening
data class SessionConfig(
    val totalDurationMinutes: Int,
    val courtsAvailable: Int,
    val estimatedMinutesPerMatch: Int = 20
) {
    val totalRounds: Int = totalDurationMinutes / estimatedMinutesPerMatch

    // Auto-calculate dynamic time cutoffs using 25% and 50% ratios
    val zone1CutoffMinutes: Int = (totalDurationMinutes * 0.25).toInt()
    val zone2CutoffMinutes: Int = (totalDurationMinutes * 0.50).toInt()
}

// Holds the resulting limits for a player
data class PlayTimeConstraints(
    val minGames: Int,
    val maxGames: Int
)

class PriorityEngine {

    /**
     * Determines game limits dynamically based on a player's arrival time.
     */
    fun getConstraintsForArrival(arrivalTimeMinutes: Int, config: SessionConfig): PlayTimeConstraints {
        val totalRounds = config.totalRounds

        return when {
            // Zone 1: Early Bird (Arrived in the first 25% of the session)
            arrivalTimeMinutes <= config.zone1CutoffMinutes -> {
                val minTarget = ceil(totalRounds * 0.45).toInt()
                PlayTimeConstraints(minGames = minTarget, maxGames = totalRounds)
            }
            // Zone 2: Mid-Session (Arrived between 25% and 50% of the session time)
            arrivalTimeMinutes <= config.zone2CutoffMinutes -> {
                val minTarget = floor(totalRounds * 0.35).toInt()
                val maxTarget = ceil(totalRounds * 0.45).toInt()
                PlayTimeConstraints(minGames = minTarget, maxGames = maxTarget)
            }
            // Zone 3: Late Arrival (Arrived after 50% of the session time has elapsed)
            else -> {
                val minTarget = floor(totalRounds * 0.20).toInt().coerceAtLeast(1)
                val maxTarget = floor(totalRounds * 0.30).toInt().coerceAtLeast(2)
                PlayTimeConstraints(minGames = minTarget, maxGames = maxTarget)
            }
        }
    }

    /**
     * Calculates a player's current matching priority score using the Relative Game Variance method.
     * Higher score means they get pushed onto the court next.
     */
    fun calculatePriorityScore(
        arrivalIndex: Int,
        gamesPlayed: Int,
        matchesSatOutConsecutively: Int,
        globalMinGames: Int
    ): Double {
        // 1. Reward early arrivals smoothly (Player 1 gets 5.0 pts, Player 11 gets 0.0)
        val arrivalBonus = maxOf(0.0, (11 - arrivalIndex) * 0.5)

        // 2. Add an aggressive bonus for sitting on the bench to prevent line starvation
        val sittingBonus = matchesSatOutConsecutively * 15.0

        // 3. Apply the game variance penalty relative strictly to the lowest active player count in the room
        val gameVariance = gamesPlayed - globalMinGames
        val gamePenalty = gameVariance * 20.0

        return arrivalBonus + sittingBonus - gamePenalty
    }
}