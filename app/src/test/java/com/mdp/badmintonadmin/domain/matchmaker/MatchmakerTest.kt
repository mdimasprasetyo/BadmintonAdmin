package com.mdp.badmintonadmin.domain.matchmaker

import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchmakerTest {

    private val matchmaker = Matchmaker()

    @Test
    fun `autoGenerateLineups prefers same-gender matches when available`() {
        // Given 4 Men and 4 Women all at the same skill level
        val players = (1..4).map { 
            createPlayer(it, "Man $it", "B", "M", it) 
        } + (5..8).map { 
            createPlayer(it, "Woman $it", "B", "W", it) 
        }

        // When generating lineups for 2 courts
        val assignments = matchmaker.autoGenerateLineups(players, 2, 0)

        // Then we should have 2 courts
        assertEquals(2, assignments.size)

        // And each court should be same-gender
        assignments.forEach { court ->
            val genders = listOf(
                court.team1.first.gender, court.team1.second.gender,
                court.team2.first.gender, court.team2.second.gender
            )
            val allSame = genders.all { it == "M" } || genders.all { it == "W" }
            assertTrue("Court ${court.courtNumber} should be same-gender but was $genders", allSame)
        }
    }

    @Test
    fun `autoGenerateLineups creates mixed matches when same-gender is not possible`() {
        // Given 6 Men and 2 Women
        val players = (1..6).map { 
            createPlayer(it, "Man $it", "B", "M", it) 
        } + (7..8).map { 
            createPlayer(it, "Woman $it", "B", "W", it) 
        }

        // When generating lineups for 2 courts
        val assignments = matchmaker.autoGenerateLineups(players, 2, 0)

        // Then we should have 2 courts
        assertEquals(2, assignments.size)

        // One court must be mixed (since there are only 2 women)
        val mixedCourts = assignments.filter { court ->
            val genders = listOf(
                court.team1.first.gender, court.team1.second.gender,
                court.team2.first.gender, court.team2.second.gender
            )
            genders.contains("M") && genders.contains("W")
        }
        assertTrue("Should have at least one mixed court", mixedCourts.isNotEmpty())
    }

    @Test
    fun `autoGenerateLineups prioritizes skill balance over gender`() {
        // Given 4 Men (S, S, D, D) and 4 Women (B, B, B, B)
        // Matchmaker should NOT group S and D together just because they are Men.
        // It should prefer mixing to keep skill balance (S/B vs S/B) if possible,
        // BUT our Matchmaker currently picks the first balanced match it finds with the top player.
        
        val p1 = createPlayer(1, "Man S1", "S", "M", 1)
        val p2 = createPlayer(2, "Man S2", "S", "M", 2)
        val p3 = createPlayer(3, "Man D1", "D", "M", 3)
        val p4 = createPlayer(4, "Man D2", "D", "M", 4)
        val w1 = createPlayer(5, "Woman B1", "B", "W", 5)
        val w2 = createPlayer(6, "Woman B2", "B", "W", 6)
        val w3 = createPlayer(7, "Woman B3", "B", "W", 7)
        val w4 = createPlayer(8, "Woman B4", "B", "W", 8)

        val players = listOf(p1, p2, p3, p4, w1, w2, w3, w4)

        // When generating lineups
        val assignments = matchmaker.autoGenerateLineups(players, 2, 0)

        // Check court 1 (contains p1 which is S level)
        val court1 = assignments.find { it.getAllPlayerIds().contains(1) }!!
        val weights1 = court1.getAllPlayerIds().map { id -> 
            players.find { it.playerId == id }?.activeTier 
        }
        
        // S (5.0) and D (1.0) gap is 4.0, which is > maxAllowedGap (2.0)
        // So p1 (S) and p3 (D) should NOT be in the same match.
        assertTrue("Skill gap too large in court 1: $weights1", !weights1.contains("D"))
    }

    @Test
    fun `two men and two women are split into mixed pairs`() {
        val players = listOf(
            createPlayer(1, "M1", "B", "M", 1),
            createPlayer(2, "M2", "B", "M", 2),
            createPlayer(3, "W1", "B", "W", 3),
            createPlayer(4, "W2", "B", "W", 4)
        )

        val court = matchmaker.autoGenerateLineups(players, 1, 0).single()

        assertTrue("Team 1 should be MW", court.team1.first.gender != court.team1.second.gender)
        assertTrue("Team 2 should be MW", court.team2.first.gender != court.team2.second.gender)
    }

    @Test
    fun `team split keeps combined tiers as even as possible`() {
        val players = listOf(
            createPlayer(1, "S", "S", "M", 1),
            createPlayer(2, "A", "A", "M", 2),
            createPlayer(3, "B", "B", "M", 3),
            createPlayer(4, "C", "C", "M", 4)
        )

        val court = matchmaker.createBalancedCourtAssignment(1, players)
        val team1 = setOf(court.team1.first.name, court.team1.second.name)

        // S+C (7) vs A+B (7) is the only perfectly even split
        assertTrue("Expected S+C vs A+B but got $team1", team1 == setOf("S", "C") || team1 == setOf("A", "B"))
    }

    @Test
    fun `fallback picks the least lopsided court instead of the next four in line`() {
        // Nobody can be balanced with the S player within the 2.0 gap, but D players are
        // first in the queue after him. The fallback should take the C players instead.
        val players = listOf(
            createPlayer(1, "S", "S", "M", 1),
            createPlayer(2, "D1", "D", "M", 2),
            createPlayer(3, "D2", "D", "M", 3),
            createPlayer(4, "D3", "D", "M", 4),
            createPlayer(5, "C1", "C", "M", 5),
            createPlayer(6, "C2", "C", "M", 6),
            createPlayer(7, "C3", "C", "M", 7)
        )

        val court = matchmaker.autoGenerateLineups(players, 1, 0).single()
        val tiers = court.getAllPlayerIds().map { id -> players.first { it.playerId == id }.activeTier }

        assertTrue("Expected S with three C players but got $tiers", tiers.count { it == "C" } == 3)
    }

    @Test
    fun `highest priority player always gets a court`() {
        val players = (1..8).map { createPlayer(it, "P$it", "B", if (it % 2 == 0) "W" else "M", it) }

        val assignments = matchmaker.autoGenerateLineups(players, 1, 0)

        assertTrue(assignments.single().getAllPlayerIds().contains(1))
    }

    private fun createPlayer(id: Int, name: String, tier: String, gender: String, arrival: Int) = PlayerMatchmakingProfile(
        playerId = id,
        name = name,
        activeTier = tier,
        gender = gender,
        arrivalIndex = arrival,
        gamesPlayed = 0,
        matchesSatOutConsecutively = 0,
        isActivePresent = true
    )
}
