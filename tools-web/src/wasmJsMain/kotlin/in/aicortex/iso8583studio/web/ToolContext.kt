package `in`.aicortex.iso8583studio.web

import androidx.compose.runtime.staticCompositionLocalOf
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool

/** The tool being shown, for the chrome's title row, badge and banner. Null on the home screen. */
val LocalToolContext = staticCompositionLocalOf<WebTool?> { null }

/** The category tree control: shows or hides the tree beside the content, or opens it as a drawer when the shell is narrow. */
data class BrowseTools(val shown: Boolean, val toggle: () -> Unit)

/** Provided by the shell; null only outside it (tests). */
val LocalBrowseTools = staticCompositionLocalOf<BrowseTools?> { null }

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
