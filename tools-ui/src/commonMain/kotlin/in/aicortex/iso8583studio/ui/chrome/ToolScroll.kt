package `in`.aicortex.iso8583studio.ui.chrome

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * A screen's own vertical scroll. On desktop this is exactly [verticalScroll]. On the web the
 * chrome owns scrolling and measures the form with unbounded height, where Compose forbids a
 * nested vertical scroll; so there the modifier is a scroll container of its own that scrolls
 * only when it is handed a bounded height (a fixed-height box inside a form) and passes the
 * content straight through otherwise, deciding inside the same measure pass.
 */
fun Modifier.toolScroll(state: ScrollState): Modifier = composed {
    if (LocalToolChrome.current.isDesktop) return@composed verticalScroll(state)
    val clamp = remember { ClampedScroll() }
    clipToBounds().scrollable(clamp, Orientation.Vertical).layout { measurable, constraints ->
        if (!constraints.hasBoundedHeight) {
            if (clamp.max != 0f) clamp.max = 0f
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        } else {
            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val height = minOf(placeable.height, constraints.maxHeight)
            val max = (placeable.height - height).toFloat().coerceAtLeast(0f)
            if (clamp.max != max) clamp.max = max
            if (clamp.offset > max) clamp.offset = max
            layout(placeable.width, height) { placeable.placeRelative(0, -clamp.offset.roundToInt()) }
        }
    }
}

/** Scroll offset clamped to what the content overflows by; reports no room to scroll when passing through. */
private class ClampedScroll : ScrollableState {
    var offset by mutableFloatStateOf(0f)
    var max by mutableFloatStateOf(0f)
    private val delegate = ScrollableState { delta ->
        val target = (offset - delta).coerceIn(0f, max)
        val consumed = offset - target
        offset = target
        consumed
    }

    override suspend fun scroll(scrollPriority: MutatePriority, block: suspend ScrollScope.() -> Unit) = delegate.scroll(scrollPriority, block)
    override fun dispatchRawDelta(delta: Float): Float = delegate.dispatchRawDelta(delta)
    override val isScrollInProgress: Boolean get() = delegate.isScrollInProgress
    override val canScrollForward: Boolean get() = offset < max
    override val canScrollBackward: Boolean get() = offset > 0f
}
