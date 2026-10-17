package `in`.aicortex.iso8583studio.web.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).toInt())

/**
 * The site's design tokens (`web/src/styles/_tokens.css`) and the derived values of its glass
 * panels (`.gp-*` / `.th-*` in `web/src/styles/bundles/tools.css`), mirrored by hand. The site
 * is dark only, so there is one set.
 */
@Immutable
data class GlassColors(
    val canvas: Color = Color(0xFF0F1017),
    val bgDeep: Color = Color(0xFF15161F),
    val bg: Color = Color(0xFF1B1D27),
    val surface: Color = Color(0xFF252836),
    val card: Color = Color(0xFF2F3142),
    val cardHi: Color = Color(0xFF383B50),
    val cardDeep: Color = Color(0xFF1E1F2E),
    val line: Color = rgba(151, 168, 214, .14f),
    val line2: Color = rgba(151, 168, 214, .07f),
    val lineHi: Color = rgba(151, 168, 214, .28f),
    val text: Color = Color(0xFFE9ECF5),
    val muted: Color = Color(0xFF9AA3BA),
    val faint: Color = Color(0xFF6A7288),
    val blue: Color = Color(0xFF1E88E5),
    val blueHi: Color = Color(0xFF6AB7FF),
    val blueHover: Color = Color(0xFF3D9BEF),
    val blueDeep: Color = Color(0xFF005CB2),
    val teal: Color = Color(0xFF26A69A),
    val tealHi: Color = Color(0xFF64D8CB),
    val green: Color = Color(0xFF43A047),
    val greenHi: Color = Color(0xFF5EC26A),
    val amber: Color = Color(0xFFFF9800),
    val amberHi: Color = Color(0xFFFFB74D),
    val red: Color = Color(0xFFE5484D),
    val redHi: Color = Color(0xFFFF8A8D),
    val onAccent: Color = Color.White,
    val windowDot: Color = Color(0xFF3C4256),
    // glass panel
    val panelBorder: Color = rgba(133, 181, 232, .24f),
    val panelTop: Color = rgba(47, 49, 66, .40f),
    val panelBottom: Color = rgba(30, 31, 46, .66f),
    val panelInsetHi: Color = rgba(255, 255, 255, .07f),
    val panelHighlight: Color = rgba(233, 236, 245, .62f),
    val dot: Color = rgba(151, 168, 214, .20f),
    val blobBlue: Color = rgba(30, 136, 229, .60f),
    val blobTeal: Color = rgba(38, 166, 154, .40f),
    val blobHi: Color = rgba(106, 183, 255, .30f),
    // fields
    val boxBg: Color = rgba(15, 16, 23, .66f),
    val boxBorder: Color = rgba(151, 168, 214, .10f),
    val boxInset: Color = rgba(0, 0, 0, .40f),
    val focusBorder: Color = rgba(106, 183, 255, .62f),
    val focusRing: Color = rgba(30, 136, 229, .16f),
    val focusGlow: Color = rgba(30, 136, 229, .30f),
    val tagBorder: Color = rgba(106, 183, 255, .28f),
    val tagBg: Color = rgba(30, 136, 229, .10f),
    val badgeBorder: Color = rgba(106, 183, 255, .22f),
    val badgeBg: Color = rgba(30, 136, 229, .16f),
    // buttons
    val ghostBorder: Color = rgba(151, 168, 214, .18f),
    val ghostBg: Color = rgba(151, 168, 214, .08f),
    val btnInsetHi: Color = rgba(255, 255, 255, .14f),
    val btnGlow: Color = rgba(30, 136, 229, .55f),
    // log and banners
    val logBg: Color = rgba(15, 16, 23, .55f),
    val logBorder: Color = rgba(151, 168, 214, .08f),
    val outWash: Color = rgba(38, 166, 154, .08f),
    val errWash: Color = rgba(229, 72, 77, .08f),
    val warnBorder: Color = rgba(255, 183, 77, .32f),
    val warnBg: Color = rgba(255, 183, 77, .10f),
    val insetBoxBg: Color = rgba(15, 16, 23, .50f),
    val insetBoxBorder: Color = rgba(151, 168, 214, .08f),
    val overlay: Color = rgba(21, 22, 31, .72f),
    // hub
    val catOnBorder: Color = rgba(106, 183, 255, .18f),
    val catOnBg: Color = rgba(30, 136, 229, .14f),
    val tileBg: Color = rgba(47, 49, 66, .28f),
    val tileBorder: Color = rgba(151, 168, 214, .07f),
    val tileOnBorder: Color = rgba(106, 183, 255, .42f),
    val tileOnBg: Color = rgba(30, 136, 229, .14f),
    val tileOnRing: Color = rgba(106, 183, 255, .14f),
    val tileOnGlow: Color = rgba(30, 136, 229, .30f),
    val hubCardTop: Color = rgba(47, 49, 66, .34f),
    val hubCardBottom: Color = rgba(30, 31, 46, .50f),
)

/** Monospace styles (IBM Plex Mono) the glass chrome uses beside the Material typography. */
@Immutable
data class GlassType(
    val mono: FontFamily,
    val value: TextStyle = TextStyle(fontFamily = mono, fontSize = 13.sp, letterSpacing = 0.02.em),
    val small: TextStyle = TextStyle(fontFamily = mono, fontSize = 11.sp),
    val log: TextStyle = TextStyle(fontFamily = mono, fontSize = 11.5.sp, lineHeight = 17.sp),
    val path: TextStyle = TextStyle(fontFamily = mono, fontSize = 10.5.sp, letterSpacing = 0.12.em),
    val details: TextStyle = TextStyle(fontFamily = mono, fontSize = 11.sp, lineHeight = 16.5.sp),
)

object GlassShapes {
    val material = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(9.dp), large = RoundedCornerShape(12.dp))
    val panel = RoundedCornerShape(16.dp)
    val box = RoundedCornerShape(9.dp)
    val tile = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(50)
}

val LocalGlassColors = staticCompositionLocalOf { GlassColors() }
val LocalGlassType = staticCompositionLocalOf<GlassType> { error("GlassTheme is not applied") }

/** Accessors for the glass tokens inside composables. */
object Glass {
    val colors: GlassColors @Composable get() = LocalGlassColors.current
    val type: GlassType @Composable get() = LocalGlassType.current
}

fun glassTypography(sans: FontFamily) = androidx.compose.material.Typography(
    defaultFontFamily = sans,
    h4 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.03).em),
    h5 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.02).em),
    h6 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = (-0.01).em),
    subtitle1 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    subtitle2 = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    body1 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    body2 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp),
    button = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.sp),
    caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp),
    overline = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.13.em),
)
