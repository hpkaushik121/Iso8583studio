package `in`.aicortex.iso8583studio.web

import androidx.compose.runtime.staticCompositionLocalOf
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool

/** The tool being shown, for the chrome's breadcrumb, badge and banner. Null on the home screen. */
val LocalToolContext = staticCompositionLocalOf<WebTool?> { null }

/** Short badge for a category, as the site's panels show it. */
fun ToolCategory.badge(): String = when (this) {
    ToolCategory.EMV -> "EMV"
    ToolCategory.CIPHER -> "CRYPTO"
    ToolCategory.KEYS -> "KEYS"
    ToolCategory.PIN -> "PIN"
    ToolCategory.CARD_VALIDATION -> "CARD"
    ToolCategory.DUKPT -> "DUKPT"
    ToolCategory.MAC -> "MAC"
    ToolCategory.CONVERTERS -> "CONVERT"
}
