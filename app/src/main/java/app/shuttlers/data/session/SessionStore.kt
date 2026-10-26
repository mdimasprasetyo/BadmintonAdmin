package app.shuttlers.data.session

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the bits of a running session that are not in Room (the running flag, court/duration
 * settings and who is on which court), so a session survives Android killing the app mid-evening.
 * Court players are stored by ID only; their live stats are re-read from session_participants.
 */
class SessionStore(context: Context) {

    data class SavedCourt(
        val courtNumber: Int,
        val team1: Pair<Int, Int>,
        val team2: Pair<Int, Int>
    )

    data class SavedSession(
        val courtsBooked: Int,
        val durationMinutes: Int,
        val courts: List<SavedCourt>
    )

    private val prefs = context.applicationContext
        .getSharedPreferences("active_session", Context.MODE_PRIVATE)

    fun isRunning(): Boolean = prefs.getBoolean(KEY_RUNNING, false)

    fun load(): SavedSession? {
        if (!isRunning()) return null
        return SavedSession(
            courtsBooked = prefs.getInt(KEY_COURTS, 2),
            durationMinutes = prefs.getInt(KEY_DURATION, 120),
            courts = decodeCourts(prefs.getString(KEY_ASSIGNMENTS, "").orEmpty())
        )
    }

    fun save(courtsBooked: Int, durationMinutes: Int, courts: List<SavedCourt>) {
        prefs.edit {
            putBoolean(KEY_RUNNING, true)
            putInt(KEY_COURTS, courtsBooked)
            putInt(KEY_DURATION, durationMinutes)
            putString(KEY_ASSIGNMENTS, encodeCourts(courts))
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val KEY_RUNNING = "running"
        private const val KEY_COURTS = "courts_booked"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_ASSIGNMENTS = "assignments"

        // Format: "court:a,b,c,d;court:a,b,c,d" (team1 = a+b, team2 = c+d)
        fun encodeCourts(courts: List<SavedCourt>): String = courts.joinToString(";") {
            "${it.courtNumber}:${it.team1.first},${it.team1.second},${it.team2.first},${it.team2.second}"
        }

        fun decodeCourts(raw: String): List<SavedCourt> = raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val number = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ids = parts[1].split(",").mapNotNull { it.toIntOrNull() }
            if (ids.size != 4) return@mapNotNull null
            SavedCourt(number, ids[0] to ids[1], ids[2] to ids[3])
        }
    }
}
