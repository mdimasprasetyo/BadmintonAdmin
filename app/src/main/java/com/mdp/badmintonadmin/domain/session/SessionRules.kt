package com.mdp.badmintonadmin.domain.session

import com.mdp.badmintonadmin.data.local.entity.SessionParticipantEntity
import com.mdp.badmintonadmin.domain.model.CourtAssignment

/** Which side won a finished match. [NONE] means "no result" (nobody gets a win or a loss). */
enum class MatchWinner { TEAM_1, TEAM_2, NONE }

/** Pure session bookkeeping, kept out of the view model so it can be unit tested. */
object SessionRules {

    /**
     * A match finished: its four players get +1 game and a bench count of 0; everyone else present
     * gets +1 bench count. The winning pair gets +1 win, the losing pair +1 loss.
     */
    fun creditFinishedMatch(
        participants: List<SessionParticipantEntity>,
        court: CourtAssignment,
        winner: MatchWinner
    ): List<SessionParticipantEntity> {
        val playingIds = court.getAllPlayerIds().toSet()
        val team1 = setOf(court.team1.first.playerId, court.team1.second.playerId)
        val team2 = setOf(court.team2.first.playerId, court.team2.second.playerId)
        val winners = when (winner) {
            MatchWinner.TEAM_1 -> team1
            MatchWinner.TEAM_2 -> team2
            MatchWinner.NONE -> emptySet()
        }
        val losers = when (winner) {
            MatchWinner.TEAM_1 -> team2
            MatchWinner.TEAM_2 -> team1
            MatchWinner.NONE -> emptySet()
        }

        return participants.map { participant ->
            when {
                participant.playerId in playingIds -> participant.copy(
                    gamesPlayed = participant.gamesPlayed + 1,
                    matchesSatOutConsecutively = 0,
                    wins = participant.wins + if (participant.playerId in winners) 1 else 0,
                    losses = participant.losses + if (participant.playerId in losers) 1 else 0
                )
                participant.isActivePresent -> participant.copy(
                    matchesSatOutConsecutively = participant.matchesSatOutConsecutively + 1
                )
                else -> participant
            }
        }
    }

    /**
     * Undo support: puts the session stats (games, bench count, presence, wins, losses) of every
     * player in [snapshot] back the way they were. Name and tier edits made since are kept, and
     * players checked in after the snapshot are left untouched.
     */
    fun restoreStats(
        current: List<SessionParticipantEntity>,
        snapshot: List<SessionParticipantEntity>
    ): List<SessionParticipantEntity> {
        val before = snapshot.associateBy { it.playerId }
        return current.map { participant ->
            val old = before[participant.playerId] ?: return@map participant
            participant.copy(
                gamesPlayed = old.gamesPlayed,
                matchesSatOutConsecutively = old.matchesSatOutConsecutively,
                isActivePresent = old.isActivePresent,
                wins = old.wins,
                losses = old.losses
            )
        }
    }

    /** One history line per player: name|games|tier|wins|losses (older sessions have 3 fields). */
    fun encodeHistoryLine(participant: SessionParticipantEntity): String =
        "${participant.name}|${participant.gamesPlayed}|${participant.activeTier}|${participant.wins}|${participant.losses}"

    data class HistoryLine(
        val name: String,
        val games: String,
        val tier: String,
        val wins: Int?,
        val losses: Int?
    )

    fun decodeHistoryLine(line: String): HistoryLine? {
        val parts = line.split("|")
        return when {
            parts.size >= 5 -> HistoryLine(parts[0], parts[1], parts[2], parts[3].toIntOrNull(), parts[4].toIntOrNull())
            parts.size == 3 -> HistoryLine(parts[0], parts[1], parts[2], null, null)
            else -> null
        }
    }
}
