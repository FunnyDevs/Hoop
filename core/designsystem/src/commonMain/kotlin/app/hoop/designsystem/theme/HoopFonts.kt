package app.hoop.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.hoop.designsystem.resources.*
import app.hoop.designsystem.token.HoopFontFamilies
import org.jetbrains.compose.resources.Font

// Static fonts (Bricolage Grotesque 700 at opsz 36, Figtree) derived from the OFL variable fonts: variable fonts are not
// supported uniformly by Compose Multiplatform on Android and iOS.
@Composable
internal fun rememberHoopFonts(): HoopFontFamilies {
    // The design uses one weight (700) for every title, the logo and big numbers, so all weights map to the same file.
    val display = FontFamily(
        Font(Res.font.bricolage_bold, FontWeight.Normal),
        Font(Res.font.bricolage_bold, FontWeight.Medium),
        Font(Res.font.bricolage_bold, FontWeight.SemiBold),
        Font(Res.font.bricolage_bold, FontWeight.Bold),
    )
    val text = FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_medium, FontWeight.Medium),
        Font(Res.font.figtree_semibold, FontWeight.SemiBold),
        Font(Res.font.figtree_bold, FontWeight.Bold),
    )
    return HoopFontFamilies(display, text)
}
