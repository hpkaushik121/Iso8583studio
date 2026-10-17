package `in`.aicortex.iso8583studio.ui.chrome

import ai.cortex.core.ValidationResult
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.aicortex.iso8583studio.logging.LogEntry

/** A tab of a calculator screen, as the screen's own tab enum describes it. */
@Immutable
data class ToolTab(val title: String, val icon: ImageVector)

/** The log pane of a screen, already resolved for the selected tab. */
@Immutable
data class ToolLog(val entries: List<LogEntry>, val onClear: () -> Unit, val label: String? = null)

/** Everything a chrome needs to frame a calculator screen. Tab state stays with the screen. */
@Immutable
data class ToolScreenSpec(
    val title: String,
    val onBack: () -> Unit,
    val tabs: List<ToolTab>? = null,
    val selectedTab: Int = 0,
    val onTabSelected: (Int) -> Unit = {},
    val log: ToolLog? = null,
    val actions: (@Composable RowScope.() -> Unit)? = null,
)

/**
 * The platform-specific presentation of the shared calculator screens: the frame around a
 * screen and the widgets inside it. Desktop uses [DesktopChrome] (the default, never provided
 * explicitly); the browser app provides its own. Screens keep their desktop code verbatim and
 * branch to `LocalToolChrome.current` when [isDesktop] is false, so the desktop app is not
 * affected by any other chrome.
 */
interface ToolChrome {
    val isDesktop: Boolean

    @Composable fun Screen(spec: ToolScreenSpec, content: @Composable () -> Unit)
    @Composable fun Card(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit)
    @Composable fun TextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier, maxLines: Int, validation: ValidationResult?, placeholder: String)
    @Composable fun Dropdown(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier)
    @Composable fun Button(text: String, onClick: () -> Unit, modifier: Modifier, isLoading: Boolean, enabled: Boolean, icon: ImageVector?)
    @Composable fun InfoDialog(title: String, onDismissRequest: () -> Unit, content: @Composable () -> Unit)
    @Composable fun LogPanel(log: ToolLog, modifier: Modifier)
    @Composable fun Tabs(tabs: List<ToolTab>, selected: Int, onSelected: (Int) -> Unit)
}

val LocalToolChrome = staticCompositionLocalOf<ToolChrome> { DesktopChrome }

/** Frames [content] with the current chrome; a convenience over `LocalToolChrome.current.Screen`. */
@Composable
fun ToolScreen(spec: ToolScreenSpec, content: @Composable () -> Unit) = LocalToolChrome.current.Screen(spec, content)
