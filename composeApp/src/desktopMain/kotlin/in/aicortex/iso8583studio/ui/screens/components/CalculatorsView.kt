package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.logging.LogEntry
import `in`.aicortex.iso8583studio.logging.LogType
import `in`.aicortex.iso8583studio.ui.screens.hostSimulator.LogPanelWithAutoScroll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.set

data class CalculatorTab(
    val title: String,
    val icon: ImageVector,
    val content: @Composable (CalculatorLogManager, CalculatorTab) -> Unit
)

// Simplified Log Manager to only show results and errors
data class CalculatorLogManager(val tabs: List<CalculatorTab>) {
    private val _logEntriesMap = mutableStateMapOf<CalculatorTab, SnapshotStateList<LogEntry>>()

    init {
        tabs.forEach { tab ->
            _logEntriesMap[tab] = mutableStateListOf()
        }
    }

    fun getLogEntries(tab: CalculatorTab): SnapshotStateList<LogEntry> {
        return _logEntriesMap[tab] ?: mutableStateListOf()
    }

    private fun addLog(tab: CalculatorTab, entry: LogEntry) {
        val logList = _logEntriesMap[tab] ?: return
        logList.add(entry)
        if (logList.size > 500) {
            logList.removeRange(400, logList.size)
        }
    }

    fun clearLogs(tab: CalculatorTab) {
        _logEntriesMap[tab]?.clear()
    }

    // Simplified logger to only show final results or errors with inputs.
    fun logOperation(
        tab: CalculatorTab,
        operation: String,
        inputs: Map<String, String>,
        result: String? = null,
        error: String? = null,
        executionTime: Long = 0L
    ) {
        // Only log if there's a result or an error to show.
        if (result == null && error == null) {
            return
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))

        val details = buildString {
            append("Inputs:\n")
            inputs.forEach { (key, value) ->
                val displayValue = if (key.contains("key", ignoreCase = true) || key.contains(
                        "pan",
                        ignoreCase = true
                    )
                ) {
                    "${value.take(8)}..." // Mask sensitive data
                } else {
                    value
                }
                append("  $key: $displayValue\n")
            }

            result?.let {
                append("\nResult:\n")
                // Handle multi-line results gracefully
                append("  ${it.replace("\n", "\n  ")}")
            }

            error?.let {
                append("\nError:\n")
                append("  Message: $it")
            }

            if (executionTime > 0) {
                append("\n\nExecution time: ${executionTime}ms")
            }
        }

        val (logType, message) = if (result != null) {
            LogType.TRANSACTION to "$operation Result"
        } else {
            LogType.ERROR to "$operation Failed"
        }

        addLog(
            tab,
            LogEntry(timestamp = timestamp, type = logType, message = message, details = details)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalculatorView(
    tabList: List<CalculatorTab>,
    onTabSelected: (Int) -> Unit
) {
    val calculatorLogManager = CalculatorLogManager(tabList)
    if (!tabList.isEmpty()) {
        var selectedTabIndex by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Professional Tab Row with custom styling - Always visible
            TabRow(
                selectedTabIndex = selectedTabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.customTabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colors.primary
                    )
                }
            ) {
                tabList.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            onTabSelected(index)
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tab.title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Main content area with a 50/50 split between operations and logs
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left panel - Crypto operations
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated content switching between different tabs
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() with
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() with
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "tab_content_transition"
                    ) { tab ->
                        tabList[tab].content(calculatorLogManager, tabList[tab])
                    }
                }

                // Right panel - Log panel for the selected tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Panel {
                        LogPanelWithAutoScroll(
                            onClearClick = {
                                calculatorLogManager.clearLogs(tabList[selectedTabIndex])
                            },
                            logEntries = calculatorLogManager.getLogEntries(tabList[selectedTabIndex])
                        )
                    }
                }
            }
        }
    } else {
        Text("No tabs available")
    }

}

private fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    composed {
        val indicatorWidth = 40.dp
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
        fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = indicatorOffset)
            .width(indicatorWidth)
    }
