package `in`.aicortex.iso8583studio.ui.screens.config.posTerminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ChipDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.BorderLight
import `in`.aicortex.iso8583studio.ui.screens.components.themedScrollbarStyle

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfigMultiSelectChipGroup(
    label: String,
    allOptions: List<String>,
    selectedOptions: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.body2)
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allOptions.forEach { option ->
                FilterChip(
                    selected = option in selectedOptions,
                    onClick = {
                        val newSet = selectedOptions.toMutableSet()
                        if (option in newSet) newSet.remove(option) else newSet.add(option)
                        onSelectionChanged(newSet)
                    },
                    leadingIcon = {
                        if (option in selectedOptions) {
                            Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = ChipDefaults.filterChipColors(
                        selectedBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                        selectedContentColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text(option)
                }
            }

        }
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
            style = themedScrollbarStyle()
        )
    }
}

@Composable
fun ConfigDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    "Dropdown"
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onValueChange(option); expanded = false }) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
fun ConfigSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colors.primary)
            Text(
                title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
        }
        Card(
            elevation = 0.dp,
            border = BorderStroke(1.dp, BorderLight),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}