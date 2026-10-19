package `in`.aicortex.iso8583studio.web

import androidx.compose.runtime.staticCompositionLocalOf
import `in`.aicortex.iso8583studio.tools.ToolCategory
import `in`.aicortex.iso8583studio.tools.WebTool

/** The tool being shown, for the chrome's title row, badge and banner. Null on the home screen. */
val LocalToolContext = staticCompositionLocalOf<WebTool?> { null }

/**
 * Opens the category tree as a drawer. Non-null only while the shell is too narrow to keep the
 * tree beside the screen, so a tool's title row shows a "Browse tools" control exactly then.
 */
val LocalBrowseTools = staticCompositionLocalOf<(() -> Unit)?> { null }

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
