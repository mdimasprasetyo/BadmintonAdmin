package com.mdp.badmintonadmin.ui.theme

import androidx.compose.ui.graphics.Color

// Court palette: deep court green-black surfaces, chalk text, one coral accent.
// Same design language as Pickles / ScoreCounter (navy + Volt there), recoloured for badminton.

// Surfaces, darkest to lightest
val CourtNight = Color(0xFF0B1A14)         // window / screen background
val CourtSurface = Color(0xFF12271E)       // cards
val CourtSurfaceHigh = Color(0xFF183328)   // raised cards, tool buttons, dialogs
val CourtSurfaceTop = Color(0xFF1F3E31)    // pressed / selected surfaces
val CourtOutline = Color(0xFF24443A)       // thin card outlines
val CourtOutlineStrong = Color(0xFF3B6454) // text field borders, dividers that must be seen

// Court green (resume card, selected states)
val CourtGreen = Color(0xFF1F5A43)
val OnCourtGreen = Color(0xFFDDF3E7)
val CourtLine = Color(0xFF7FC4A3)          // chalk-green line colour for small highlights

// Text
val Chalk = Color(0xFFEEF5F0)
val Muted = Color(0xFF9DB8AB)
val Faint = Color(0xFF6E8C7F)

// Accent: coral, used sparingly for primary actions, pills and highlights
val Coral = Color(0xFFFF6B5A)
val OnCoral = Color(0xFF2A0E0A)
val CoralContainer = Color(0xFF4A2019)
val OnCoralContainer = Color(0xFFFFDAD3)

// Results and states
val WinGreen = Color(0xFF6FD69C)
val OnWinGreen = Color(0xFF062414)
val Danger = Color(0xFFFF5C7A)             // destructive actions (raspberry, distinct from coral)
val OnDanger = Color(0xFF3B0614)
val DangerContainer = Color(0xFF4A1524)

// Gender chips: cool teal-blue vs soft rose, tinted containers with light text (AA on both)
val GenderMale = Color(0xFF133944)
val OnGenderMale = Color(0xFF8EDCEE)
val GenderFemale = Color(0xFF46212E)
val OnGenderFemale = Color(0xFFFFB1C8)

// Skill tiers S > A > X > B > C > D
val TierS = Color(0xFFFFCF5C)
val TierA = Color(0xFFFF9F70)
val TierB = Color(0xFF7FDDA9)
val TierC = Color(0xFF7CCBE6)
val TierD = Color(0xFFB9ACEB)
val TierX = Color(0xFFB7C9C0)

fun tierColor(tier: String): Color = when (tier.uppercase()) {
    "S" -> TierS
    "A" -> TierA
    "B" -> TierB
    "C" -> TierC
    "D" -> TierD
    else -> TierX
}
