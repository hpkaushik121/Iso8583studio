package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry

/** A vertical scrollbar beside a lazy list: real on desktop, nothing in the browser. */
@Composable
expect fun PlatformVerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)

/** How a calculator screen's log panel is drawn: the desktop app installs the host simulator's full panel. */
fun interface ToolLogPanel {
    @Composable
    fun Render(label: String?, onClearClick: () -> Unit, logEntries: List<LogEntry>)
}

/** Defaults to the compact panel; `AppTheme` on desktop provides the richer `hostSimulator` one. */
val LocalToolLogPanel = staticCompositionLocalOf<ToolLogPanel> { ToolLogPanel { label, onClear, entries -> CompactLogPanel(label, onClear, entries) } }

/**
 * The log panel every calculator screen renders on its right-hand side. Which panel that is comes
 * from [LocalToolLogPanel]: the desktop app routes it to the host simulator's full panel (type
 * filter, auto-scroll toggle, selectable rows), exactly as before the screens moved to this module;
 * the browser and the tests get [CompactLogPanel].
 */
@Composable
fun LogPanelWithAutoScroll(
    label: String? = null,
    onClearClick: () -> Unit,
    logEntries: List<LogEntry> = emptyList(),
) = LocalToolLogPanel.current.Render(label, onClearClick, logEntries)

/**
 * The in-memory compact log panel. Follows the tail while the user is at the bottom, stops
 * following once they scroll up.
 */
@Composable
fun CompactLogPanel(
    label: String? = null,
    onClearClick: () -> Unit,
    logEntries: List<LogEntry> = emptyList(),
) {
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(logEntries.size, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && logEntries.isNotEmpty()) {
            listState.scrollToItem(logEntries.size - 1)
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            logEntries.isEmpty() || last >= logEntries.size - 2
        }
    }
    var hasScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            hasScrolled = true
        } else if (hasScrolled) {
            isAutoScrollEnabled = isAtBottom
        }
    }
    LaunchedEffect(logEntries.isEmpty()) {
        if (logEntries.isEmpty()) isAutoScrollEnabled = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label ?: "Logs",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${logEntries.size}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClearClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear logs",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 12.dp)
                ) {
                    items(logEntries, key = { it.id }) { entry -> LogEntryRow(entry) }
                }
            }
            PlatformVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxSize())
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = entry.type.icon,
            contentDescription = entry.type.displayName,
            tint = entry.type.color,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = entry.type.color
                )
            }
            entry.details?.takeIf { it.isNotBlank() }?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
