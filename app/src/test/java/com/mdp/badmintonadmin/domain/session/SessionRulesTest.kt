package com.mdp.badmintonadmin.domain.session

import com.mdp.badmintonadmin.data.local.Migrations
import com.mdp.badmintonadmin.data.local.entity.SessionParticipantEntity
import com.mdp.badmintonadmin.domain.model.CourtAssignment
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {

    private fun participant(id: Int, games: Int = 0, bench: Int = 0, present: Boolean = true, wins: Int = 0, losses: Int = 0) =
        SessionParticipantEntity(
            playerId = id,
            name = "P$id",
            activeTier = "B",
            gender = "M",
            arrivalIndex = id,
            gamesPlayed = games,
            matchesSatOutConsecutively = bench,
            isActivePresent = present,
            wins = wins,
            losses = losses
        )

    private fun profile(id: Int) = PlayerMatchmakingProfile(
        playerId = id, name = "P$id", activeTier = "B", gender = "M",
        arrivalIndex = id, gamesPlayed = 0, matchesSatOutConsecutively = 0
    )

    private val court = CourtAssignment(1, profile(1) to profile(2), profile(3) to profile(4))
    private val everyone = listOf(
        participant(1), participant(2), participant(3), participant(4),
        participant(5, bench = 1), participant(6, present = false)
    )

    @Test
    fun `team 1 win credits a win to team 1 and a loss to team 2`() {
        val result = SessionRules.creditFinishedMatch(everyone, court, MatchWinner.TEAM_1).associateBy { it.playerId }

        listOf(1, 2).forEach { assertEquals(1, result.getValue(it).wins); assertEquals(0, result.getValue(it).losses) }
        listOf(3, 4).forEach { assertEquals(0, result.getValue(it).wins); assertEquals(1, result.getValue(it).losses) }
        (1..4).forEach {
            assertEquals(1, result.getValue(it).gamesPlayed)
            assertEquals(0, result.getValue(it).matchesSatOutConsecutively)
        }
        assertEquals(2, result.getValue(5).matchesSatOutConsecutively) // present and waiting
        assertEquals(0, result.getValue(6).matchesSatOutConsecutively) // away: untouched
    }

    @Test
    fun `no result finishes the match without wins or losses`() {
        val result = SessionRules.creditFinishedMatch(everyone, court, MatchWinner.NONE)
        assertTrue(result.all { it.wins == 0 && it.losses == 0 })
        assertEquals(4, result.count { it.gamesPlayed == 1 })
    }

    @Test
    fun `restoreStats undoes a finished match but keeps later edits and new players`() {
        val after = SessionRules.creditFinishedMatch(everyone, court, MatchWinner.TEAM_2)
            .map { if (it.playerId == 1) it.copy(name = "Renamed", activeTier = "A") else it } + participant(7)

        val restored = SessionRules.restoreStats(after, everyone).associateBy { it.playerId }

        everyone.forEach { before ->
            val now = restored.getValue(before.playerId)
            assertEquals(before.gamesPlayed, now.gamesPlayed)
            assertEquals(before.matchesSatOutConsecutively, now.matchesSatOutConsecutively)
            assertEquals(before.wins, now.wins)
            assertEquals(before.losses, now.losses)
            assertEquals(before.isActivePresent, now.isActivePresent)
        }
        assertEquals("Renamed", restored.getValue(1).name)
        assertEquals("A", restored.getValue(1).activeTier)
        assertTrue(restored.containsKey(7))
    }

    @Test
    fun `history lines round trip and old three-field lines still parse`() {
        val line = SessionRules.encodeHistoryLine(participant(1, games = 3, wins = 2, losses = 1))
        assertEquals(SessionRules.HistoryLine("P1", "3", "B", 2, 1), SessionRules.decodeHistoryLine(line))

        val old = SessionRules.decodeHistoryLine("Ana|4|A")!!
        assertEquals("Ana", old.name)
        assertNull(old.wins)
        assertNull(SessionRules.decodeHistoryLine("garbage"))
    }

    @Test
    fun `migration 4 to 5 adds the session result columns with a zero default`() {
        val sql = Migrations.MIGRATION_4_5_STATEMENTS.joinToString(";")
        assertTrue(sql.contains("ADD COLUMN wins INTEGER NOT NULL DEFAULT 0"))
        assertTrue(sql.contains("ADD COLUMN losses INTEGER NOT NULL DEFAULT 0"))
        assertEquals(4, Migrations.MIGRATION_4_5.startVersion)
        assertEquals(5, Migrations.MIGRATION_4_5.endVersion)
    }
}
