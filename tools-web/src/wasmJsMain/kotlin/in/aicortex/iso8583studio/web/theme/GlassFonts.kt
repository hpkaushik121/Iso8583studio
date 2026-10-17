package `in`.aicortex.iso8583studio.web.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import iso8583studio.web.res.Res
import iso8583studio.web.res.ibm_plex_mono_medium
import iso8583studio.web.res.ibm_plex_mono_regular
import iso8583studio.web.res.ibm_plex_mono_semibold
import iso8583studio.web.res.ibm_plex_sans_medium
import iso8583studio.web.res.ibm_plex_sans_regular
import iso8583studio.web.res.ibm_plex_sans_semibold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

/**
 * Loads the bundled IBM Plex faces; null until all six are ready. The app shows nothing before
 * that, so no text is ever drawn in the fallback font.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberGlassFonts(): GlassFonts? {
    val faces = listOf(
        preloadFont(Res.font.ibm_plex_sans_regular, FontWeight.Normal).value,
        preloadFont(Res.font.ibm_plex_sans_medium, FontWeight.Medium).value,
        preloadFont(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold).value,
        preloadFont(Res.font.ibm_plex_mono_regular, FontWeight.Normal).value,
        preloadFont(Res.font.ibm_plex_mono_medium, FontWeight.Medium).value,
        preloadFont(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold).value,
    )
    if (faces.any { it == null }) return null
    val loaded = faces.map { it as Font }
    return remember(loaded) { GlassFonts(sans = FontFamily(loaded[0], loaded[1], loaded[2]), mono = FontFamily(loaded[3], loaded[4], loaded[5])) }
}
