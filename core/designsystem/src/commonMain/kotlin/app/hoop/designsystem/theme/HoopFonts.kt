package app.hoop.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.hoop.designsystem.resources.*
import app.hoop.designsystem.token.HoopFontFamilies
import org.jetbrains.compose.resources.Font

// Static fonts (Fraunces opsz 72, Figtree) derived from the OFL variable fonts: variable fonts are not
// supported uniformly by Compose Multiplatform on Android and iOS.
@Composable
internal fun rememberHoopFonts(): HoopFontFamilies {
    val display = FontFamily(
        Font(Res.font.fraunces_regular, FontWeight.Normal),
        Font(Res.font.fraunces_medium, FontWeight.Medium),
        Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
        Font(Res.font.fraunces_bold, FontWeight.Bold),
    )
    val text = FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_medium, FontWeight.Medium),
        Font(Res.font.figtree_semibold, FontWeight.SemiBold),
        Font(Res.font.figtree_bold, FontWeight.Bold),
    )
    return HoopFontFamilies(display, text)
}
