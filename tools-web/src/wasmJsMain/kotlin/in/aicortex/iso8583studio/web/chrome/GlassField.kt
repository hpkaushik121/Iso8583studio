package `in`.aicortex.iso8583studio.web.chrome

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.web.theme.Glass
import `in`.aicortex.iso8583studio.web.theme.GlassShapes

private val TAG_SUFFIX = Regex("""^(.*?)\s*\(\s*(?:tag\s+)?([0-9A-Fa-f]*[0-9][0-9A-Fa-f]*)\s*\)\s*$""")
private val HINT_SUFFIX = Regex("""^(.*?)\s*\(([^()]{1,40})\)\s*$""")

/** A screen label split into the parts the `.gp-field` label row shows. */
internal class FieldLabel(val text: String, val tag: String?, val hint: String?, val optional: Boolean)

internal fun parseLabel(raw: String): FieldLabel {
    var label = raw.trim()
    var optional = false
    if (label.contains("optional", ignoreCase = true)) {
        optional = true
        label = label.replace(Regex("""\s*[(,]?\s*optional\)?""", RegexOption.IGNORE_CASE), "").trim().trimEnd(',', '(').trim()
    }
    TAG_SUFFIX.find(label)?.let { m -> if (m.groupValues[2].length in 2..8) return FieldLabel(m.groupValues[1].trim(), m.groupValues[2].uppercase(), null, optional) }
    HINT_SUFFIX.find(label)?.let { m -> return FieldLabel(m.groupValues[1].trim(), null, m.groupValues[2].trim(), optional) }
    return FieldLabel(label, null, null, optional)
}

@Composable
internal fun LabelRow(label: FieldLabel) {
    val c = Glass.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().height(16.dp)) {
        Text(label.text, style = MaterialTheme.typography.subtitle2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        label.tag?.let {
            Text(it, style = Glass.type.small, color = c.blueHi, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.tagBg).border(1.dp, c.tagBorder, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
        }
        label.hint?.let { Text(it, style = MaterialTheme.typography.caption, color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (label.optional) Text("optional", style = MaterialTheme.typography.caption, color = c.faint)
    }
}

/** The `.gp-field`: label row, dark inset box with a mono value and a character count, message below. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    validation: ValidationResult? = null,
    placeholder: String = "",
) {
    val c = Glass.colors
    val parsed = remember(label) { parseLabel(label) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val state = validation?.state ?: ValidationState.EMPTY
    val isError = state == ValidationState.ERROR
    val isWarning = state == ValidationState.WARNING
    val borderColour = when {
        isError -> c.red.copy(alpha = 0.6f)
        isWarning -> c.amberHi.copy(alpha = 0.6f)
        focused -> c.focusBorder
        hovered -> c.lineHi
        else -> c.boxBorder
    }
    val single = maxLines == 1
    val count = value.count { !it.isWhitespace() }
    val countText = validation?.helperText?.takeIf { state == ValidationState.VALID && it.isNotEmpty() } ?: count.toString()
    val countColour = when { isError -> c.redHi; count == 0 -> c.faint; else -> c.greenHi }

    Column(modifier = modifier) {
        LabelRow(parsed)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Glass.type.value.copy(color = c.text),
            cursorBrush = SolidColor(c.blueHi),
            singleLine = single,
            maxLines = maxLines,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focused && !isError) Modifier.glow(c.focusGlow, 9.dp, 20.dp).ring(c.focusRing, 3.dp, 9.dp) else Modifier)
                .clip(GlassShapes.box)
                .background(c.boxBg)
                .border(1.dp, borderColour, GlassShapes.box)
                .insetTop(c.boxInset)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Text)
                .heightIn(min = if (single) 36.dp else 64.dp),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = if (single) 0.dp else 10.dp).heightIn(min = if (single) 36.dp else 44.dp),
                    verticalAlignment = if (single) Alignment.CenterVertically else Alignment.Top,
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(placeholder.ifEmpty { "…" }, style = Glass.type.value, color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        inner()
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(countText, style = Glass.type.small, color = countColour, modifier = Modifier.padding(top = if (single) 0.dp else 2.dp))
                }
            },
        )
        val message = validation?.message.orEmpty()
        if (message.isNotEmpty() && (isError || isWarning)) {
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isError) Icons.Default.Error else Icons.Default.Warning, contentDescription = null, tint = if (isError) c.redHi else c.amberHi, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(message, style = MaterialTheme.typography.caption, color = if (isError) c.redHi else c.amberHi)
            }
        }
    }
}

/** The same box as a read-only chooser with a dark menu the width of the box. */
@Composable
fun GlassDropdown(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Glass.colors
    val parsed = remember(label) { parseLabel(label) }
    var expanded by remember { mutableStateOf(false) }
    var boxWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val caret by animateFloatAsState(if (expanded) 180f else 0f, label = "caret")
    Column(modifier = modifier) {
        LabelRow(parsed)
        Spacer(Modifier.height(5.dp))
        Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { boxWidth = it.size.width }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.ring(c.focusRing, 3.dp, 9.dp) else Modifier)
                    .clip(GlassShapes.box)
                    .background(c.boxBg)
                    .border(1.dp, if (expanded) c.focusBorder else if (hovered) c.lineHi else c.boxBorder, GlassShapes.box)
                    .insetTop(c.boxInset)
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
                    .height(36.dp)
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, style = Glass.type.value, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = c.faint, modifier = Modifier.size(14.dp).rotate(caret))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(density) { boxWidth.toDp() }).heightIn(max = 280.dp).border(1.dp, c.lineHi, GlassShapes.box),
            ) {
                options.forEachIndexed { index, option ->
                    val selected = option == value
                    DropdownMenuItem(onClick = { onSelectionChanged(index); expanded = false }) {
                        Text(option, style = MaterialTheme.typography.body2, color = if (selected) c.blueHi else c.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = c.blueHi, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
