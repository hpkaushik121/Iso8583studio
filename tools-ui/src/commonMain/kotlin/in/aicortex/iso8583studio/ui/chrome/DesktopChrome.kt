package `in`.aicortex.iso8583studio.ui.chrome

import ai.cortex.core.ValidationResult
import ai.cortex.core.ValidationState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.screens.components.AppBarWithBack
import `in`.aicortex.iso8583studio.ui.screens.components.FixedOutlinedTextField
import `in`.aicortex.iso8583studio.ui.screens.components.LogPanelWithAutoScroll
import `in`.aicortex.iso8583studio.ui.screens.components.Panel

/**
 * The desktop chrome: exactly the look the desktop app has always had. The component bodies
 * are the former `components/Common.kt` bodies, moved here unchanged; [Screen] and [Tabs]
 * reproduce the Scaffold / TabRow / 50-50 split the screens draw inline. Desktop never
 * provides a chrome, so this object is the default of [LocalToolChrome].
 */
object DesktopChrome : ToolChrome {
    override val isDesktop: Boolean get() = true

    @Composable
    override fun Screen(spec: ToolScreenSpec, content: @Composable () -> Unit) {
        Scaffold(
            topBar = { AppBarWithBack(title = spec.title, onBackClick = spec.onBack, actions = spec.actions ?: {}) },
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                spec.tabs?.let { Tabs(it, spec.selectedTab, spec.onTabSelected) }
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
                    spec.log?.let { log ->
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) { LogPanel(log, Modifier) }
                    }
                }
            }
        }
    }

    @Composable
    override fun Tabs(tabs: List<ToolTab>, selected: Int, onSelected: (Int) -> Unit) {
        TabRow(
            selectedTabIndex = selected,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(modifier = Modifier.desktopTabIndicatorOffset(tabPositions[selected]), height = 3.dp, color = MaterialTheme.colors.primary)
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selected == index,
                    onClick = { onSelected(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tab.title, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    override fun LogPanel(log: ToolLog, modifier: Modifier) {
        Panel(modifier = modifier) {
            LogPanelWithAutoScroll(label = log.label, onClearClick = log.onClear, logEntries = log.entries)
        }
    }

    @Composable
    override fun Card(title: String, subtitle: String, icon: ImageVector, onInfoClick: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 6.dp,
            backgroundColor = MaterialTheme.colors.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (onInfoClick != null) {
                        IconButton(onClick = onInfoClick) {
                            Icon(Icons.Default.Info, contentDescription = "Information", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
}

    @Composable
    override fun TextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier, maxLines: Int, validation: ValidationResult?, placeholder: String) {
        val validation = validation ?: ValidationResult(ValidationState.EMPTY)

        Column(modifier = modifier) {
            FixedOutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = maxLines,
                singleLine = maxLines == 1,
                isError = validation.state == ValidationState.ERROR,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = when (validation.state) {
                        ValidationState.VALID -> MaterialTheme.colors.primary
                        ValidationState.WARNING -> Color(0xFFFFC107)
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    },
                    unfocusedBorderColor = when (validation.state) {
                        ValidationState.VALID -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        ValidationState.WARNING -> Color(0xFFFFC107)
                        ValidationState.ERROR -> MaterialTheme.colors.error
                        ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    }
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (validation.message.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val (icon, color) = when (validation.state) {
                            ValidationState.ERROR -> Icons.Default.Error to MaterialTheme.colors.error
                            ValidationState.WARNING -> Icons.Default.Warning to Color(0xFF856404)
                            else -> null to MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        }

                        icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = validation.message,
                            color = color,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (validation.helperText.isNotEmpty()) {
                    Text(
                        text = validation.helperText,
                        color = when (validation.state) {
                            ValidationState.VALID -> SuccessGreen
                            ValidationState.WARNING -> Color(0xFF856404)
                            ValidationState.ERROR -> MaterialTheme.colors.error
                            ValidationState.EMPTY -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        },
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
}

    @Composable
    override fun Dropdown(label: String, value: String, options: List<String>, onSelectionChanged: (Int) -> Unit, modifier: Modifier) {

        var expanded by remember { mutableStateOf(false) }
        var textFieldWidth by remember { mutableStateOf(0) }
        val density = LocalDensity.current

        Box(modifier = modifier.onGloballyPositioned { coordinates ->
            textFieldWidth = coordinates.size.width
        }) {
            FixedOutlinedTextField(
                value = value,
                onValueChange = {},
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    disabledTextColor = MaterialTheme.colors.onSurface,
                    disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable { expanded = !expanded }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { textFieldWidth.toDp() })
                    .heightIn(max = 300.dp)
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        onClick = {
                            onSelectionChanged(index)
                            expanded = false
                        }
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }
}

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Button(text: String, onClick: () -> Unit, modifier: Modifier, isLoading: Boolean, enabled: Boolean, icon: ImageVector?) {

        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) with fadeOut(animationSpec = tween(150))
                },
                label = "button_anim"
            ) { loading ->
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = text,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
}

    @Composable
    override fun InfoDialog(title: String, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                SelectionContainer {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        content()
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text("OK")
                }
            },
            shape = MaterialTheme.shapes.medium
        )
}
}

/** The 40dp centred tab indicator every desktop screen draws. */
fun Modifier.desktopTabIndicatorOffset(currentTabPosition: TabPosition): Modifier = composed {
    val indicatorWidth = 40.dp
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left + (currentTabWidth - indicatorWidth) / 2
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(indicatorWidth)
}
