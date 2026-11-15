package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Font families: Bricolage Grotesque for headings, Figtree for body text. */
@Immutable
class HoopFontFamilies(val display: FontFamily, val text: FontFamily)

/** Text styles named by role. No scattered `fontSize` values in components. */
@Immutable
class HoopTypography(
    val displayLarge: TextStyle,
    val headline: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,
)

/** Slightly tighter tracking of the display font, as in the design (-0.015em). */
private val DISPLAY_TRACKING = (-0.015).em

internal fun hoopTypography(fonts: HoopFontFamilies) = HoopTypography(
    displayLarge = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = DISPLAY_TRACKING),
    headline = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 35.sp, letterSpacing = DISPLAY_TRACKING),
    titleLarge = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = DISPLAY_TRACKING),
    titleMedium = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 19.sp, letterSpacing = DISPLAY_TRACKING),
    bodyLarge = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    caption = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
