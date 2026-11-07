package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Font families: Fraunces for headings, Figtree for body text. */
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

internal fun hoopTypography(fonts: HoopFontFamilies) = HoopTypography(
    displayLarge = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp),
    headline = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 35.sp),
    titleLarge = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    caption = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
