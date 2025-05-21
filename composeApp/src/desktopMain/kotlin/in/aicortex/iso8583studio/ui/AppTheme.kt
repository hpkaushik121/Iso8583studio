package `in`.aicortex.iso8583studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Define brand colors
val Purple80 = Color(0xFF6650a4)
val PurpleGrey80 = Color(0xFFccc2dc)
val Pink80 = Color(0xFFefb8c8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ISO8583Studio specific colors
val PrimaryBlue = Color(0xFF1e88e5)
val PrimaryBlueLight = Color(0xFF6ab7ff)
val PrimaryBlueDark = Color(0xFF005cb2)
val AccentTeal = Color(0xFF26a69a)
val AccentTealLight = Color(0xFF64d8cb)
val AccentTealDark = Color(0xFF00766c)
val BackgroundLight = Color(0xFFf5f7fa)
val BackgroundDark = Color(0xFF252836)
val CardLight = Color(0xFFffffff)
val CardDark = Color(0xFF2F3142)
val ErrorRed = Color(0xFFe53935)
val SuccessGreen = Color(0xFF43a047)
val WarningYellow = Color(0xFFffb300)
val BorderLight = Color(0xFFe0e0e0)
val BorderDark = Color(0xFF3d3d3d)
val TextPrimaryLight = Color(0xFF212121)
val TextSecondaryLight = Color(0xFF757575)
val TextPrimaryDark = Color(0xFFeceff1)
val TextSecondaryDark = Color(0xFFb0bec5)

private val DarkColorPalette = darkColors(
    primary = PrimaryBlue,
    primaryVariant = PrimaryBlueDark,
    secondary = AccentTeal,
    background = BackgroundDark,
    surface = CardDark,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = Color.White
)

private val LightColorPalette = lightColors(
    primary = PrimaryBlue,
    primaryVariant = PrimaryBlueDark,
    secondary = AccentTeal,
    secondaryVariant = AccentTealDark,
    background = BackgroundLight,
    surface = CardLight,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onError = Color.White
)


// Set of Material typography styles to start with
val Typography = Typography(
    h4 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    button = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    overline = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
    )
)

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)
@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}