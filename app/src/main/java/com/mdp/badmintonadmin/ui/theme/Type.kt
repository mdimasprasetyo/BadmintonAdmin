package com.mdp.badmintonadmin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mdp.badmintonadmin.R

// Bundled (OFL) so the app stays fully offline. Same fonts as Pickles and ScoreCounter.

/** Plus Jakarta Sans: body and interface text. */
val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold)
)

/** Barlow Condensed: big numbers, court numbers, tier letters and headers. */
val Barlow = FontFamily(
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp, fontFeatureSettings = "tnum"),
    displayMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 50.sp, fontFeatureSettings = "tnum"),
    displaySmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 40.sp, fontFeatureSettings = "tnum"),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp)
)

/** App-specific styles on top of the Material scale. */
object AppText {
    /** Uppercase, letter-spaced small section label ("WAITING LOUNGE"). Pass text already uppercased. */
    val SectionLabel = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.em)

    /** Wide-tracked caps used for the app title. */
    val TitleCaps = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.em)

    /** Condensed uppercase header ("COURT", "VS", "TEAM 1"). */
    val CondensedLabel = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 16.sp, letterSpacing = 0.1.em)

    /** Big condensed numbers (court number, stat tiles). */
    val NumberLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 44.sp, fontFeatureSettings = "tnum")
    val NumberMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp, fontFeatureSettings = "tnum")
    val NumberSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum")
}
