package `in`.aicortex.iso8583studio.web.chrome

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import kotlin.math.sqrt

// Drawing helpers for the glass look. tools-web is wasm-only, so skiko is used directly for the
// few blurred glows; everything else is gradients and alpha, which cost nothing extra.

/** A soft glow under a rounded rectangle (CSS `box-shadow: 0 0 <blur> color`). */
fun Modifier.glow(color: Color, radius: Dp, blur: Dp, inflate: Dp = 0.dp): Modifier = drawBehind {
    val paint = Paint().apply {
        this.color = color.toArgb()
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur.toPx() / 2f)
    }
    val i = inflate.toPx()
    drawContext.canvas.nativeCanvas.drawRRect(RRect.makeLTRB(-i, -i, size.width + i, size.height + i, radius.toPx()), paint)
}

/** A 3dp inner shadow along the top edge (CSS `inset 0 1px 3px rgba(0,0,0,.4)`). */
fun Modifier.insetTop(color: Color, height: Dp = 3.dp): Modifier = drawWithContent {
    drawContent()
    drawRect(Brush.verticalGradient(0f to color, 1f to Color.Transparent, endY = height.toPx()), size = Size(size.width, height.toPx()))
}

/** A ring outside the shape (CSS `0 0 0 <width> color`). */
fun Modifier.ring(color: Color, width: Dp, radius: Dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-w / 2f, -w / 2f),
        size = Size(size.width + w, size.height + w),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx() + w / 2f),
        style = Stroke(w),
    )
}

/** The panel's dotted grid, fading out from an ellipse centred at (72%, 24%). Positions are cached per size. */
fun Modifier.dotGrid(color: Color, step: Dp = 14.dp): Modifier = drawWithCache {
    val s = step.toPx()
    val dots = ArrayList<Pair<Offset, Float>>()
    var y = s / 2
    while (y < size.height) {
        var x = s / 2
        while (x < size.width) {
            val dx = (x / size.width - 0.72f) / 0.9f
            val dy = (y / size.height - 0.24f) / 0.9f
            val d = sqrt(dx * dx + dy * dy)
            val m = 1f - smoothstep(0.18f, 0.72f, d)
            if (m > 0.03f) dots += Offset(x, y) to m
            x += s
        }
        y += s
    }
    onDrawBehind { for ((o, m) in dots) drawCircle(color.copy(alpha = color.alpha * m), 1f, o) }
}

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The three soft colour blobs behind a panel (`.gp-bg`), drawn statically. */
fun DrawScope.drawBlobs(blue: Color, teal: Color, hi: Color) {
    val w = size.width; val h = size.height
    fun blob(cx: Float, cy: Float, r: Float, c: Color) =
        drawCircle(Brush.radialGradient(0f to c, 0.62f to Color.Transparent, center = Offset(cx, cy), radius = r), r, Offset(cx, cy))
    val r = w * 0.48f
    blob(-0.10f * w + r / 2, -0.28f * h + r / 2, r, blue)
    blob(w + 0.12f * w - r / 2, h + 0.32f * h - r / 2, r, teal)
    blob(0.36f * w + w * 0.18f, 0.22f * h + w * 0.18f, w * 0.36f, hi)
}
