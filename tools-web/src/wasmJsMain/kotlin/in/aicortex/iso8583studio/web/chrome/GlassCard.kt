package `in`.aicortex.iso8583studio.web.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

/** A section inside the glass panel: tinted icon square, title and subtitle, then the content. The panel is the card. */
@Composable
fun GlassCard(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val c = Glass.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(c.tagBg).border(1.dp, c.tagBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = c.blueHi, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.subtitle1, color = c.text)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.caption, color = c.muted)
            }
            onInfoClick?.let { GhostIcon(Icons.Default.Info, "About this calculator", size = 22.dp, onClick = it) }
        }
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(18.dp))
    }
}

/** A glass sheet over a dark scrim. */
@Composable
fun GlassInfoDialog(title: String, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val c = Glass.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 600.dp)
                .clip(GlassShapes.panel)
                .drawBehind {
                    drawRoundRect(Brush.linearGradient(listOf(c.cardHi, c.cardDeep), start = Offset.Zero, end = Offset(size.width * 0.35f, size.height)), cornerRadius = CornerRadius(16.dp.toPx()))
                }
                .border(1.dp, c.panelBorder, GlassShapes.panel),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = c.blueHi, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.h6, color = c.text, modifier = Modifier.weight(1f))
                GhostIcon(Icons.Default.Close, "Close", size = 24.dp, onClick = onDismissRequest)
            }
            Hairline()
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body1.copy(color = c.muted), LocalContentColor provides c.muted) {
                SelectionContainer {
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
                }
            }
            Hairline()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                GlassButton("Close", onClick = onDismissRequest, primary = false)
            }
        }
    }
}
