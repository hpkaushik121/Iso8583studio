package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The browser build relies on wheel and touch scrolling; no scrollbar is drawn. */
@Composable
actual fun PlatformVerticalScrollbar(state: LazyListState, modifier: Modifier) = Unit
