package `in`.aicortex.iso8583studio.ui.screens.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.StudioVersion
import `in`.aicortex.iso8583studio.ui.screens.EnhancedStudioTool
import `in`.aicortex.iso8583studio.ui.screens.HomeScreenViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*



/**
 * Re-imagined HomeScreen focusing on a dashboard-like experience.
 * It uses a LazyVerticalGrid for a more dynamic and scalable layout.
 */
@Composable
fun OverviewContent(viewModel: HomeScreenViewModel) {


    LaunchedEffect(Unit) {
        viewModel.overviewVisibility.value = true
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        // MODIFICATION: Added 80.dp to bottom padding to avoid overlap with a FAB or bottom bar.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Span the full width for the header
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = viewModel.overviewVisibility.value,
                enter = slideInVertically(
                    initialOffsetY = { -60 },
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(700))
            ) {
                HeaderDashboard()
            }
        }

        // Span the full width for the "Quick Access" section title
        item(span = { GridItemSpan(maxLineSpan) }) {
            DashboardSectionTitle("🔥 Quick Access", "Your most frequently used tools")
        }

        items(viewModel.popularTools.size) { index ->
            StudioToolCard(tool = viewModel.popularTools[index])
        }

        // Span the full width for the "Daily Wisdom" quote
        item(span = { GridItemSpan(maxLineSpan) }) {
            DailyWisdomQuote(viewModel)
        }

        // Span the full width for the "What's New" section title
        if (viewModel.newTools.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DashboardSectionTitle("🚀 What's New", "Check out the latest additions")
            }
            items(viewModel.newTools.size) { index ->
                StudioToolCard(tool = viewModel.newTools[index])
            }
        }
    }
}

@Composable
private fun HeaderDashboard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.05f),
                            MaterialTheme.colors.secondary.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                    )
                )
                .padding(24.dp)
        ) {
            // Main Title and Version
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "ISO8583 Studio",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        "Payment Testing & Simulation Platform",
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
                Card(
                    elevation = 0.dp,
                    backgroundColor = MaterialTheme.colors.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "v${StudioVersion}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 20.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
            )

            // Platform Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem("47+", "Tools", Icons.Default.Build, Modifier.weight(1f))
                MetricItem("8", "Simulators", Icons.Default.Router, Modifier.weight(1f))
                MetricItem("15+", "HSM Vendors", Icons.Default.Security, Modifier.weight(1f))
                MetricItem("100%", "ISO Compliant", Icons.Default.Verified, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricItem(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DashboardSectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StudioToolCard(tool: EnhancedStudioTool) {
    val interactionSource = remember { MutableInteractionSource() }
    var isScaled by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (isScaled) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toolCardScale"
    ).value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isScaled = !isScaled // Example interaction
            },
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row {
            // Status Indicator Border
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(tool.status.color)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MODIFICATION: Removed the star icon from this Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tool Icon and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tool.name,
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // Add a minimum height to ensure consistent card size even if description is short
                    modifier = Modifier.heightIn(min = 36.dp)
                )

                // MODIFICATION: Footer with only the Action Arrow. Usage count is removed.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End, // Aligns the arrow to the right
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to ${tool.name}",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun DailyWisdomQuote(viewModel: HomeScreenViewModel) {
    val today = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) }
    val quoteIndex = remember(today) {
        today.hashCode().mod(viewModel.paymentQuotes.size).let {
            if (it < 0) it + viewModel.paymentQuotes.size else it
        }
    }
    val todayQuote = viewModel.paymentQuotes[quoteIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colors.secondary)
            Text(
                "Daily Wisdom",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.secondary
            )
        }

        // Typewriter effect from original code
        TypewriterText(
            text = todayQuote,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            fontStyle = FontStyle.Italic,
            lineHeight = 24.sp,
            viewModel = viewModel
        )
    }
}

// Typewriter composable from the original code (unchanged)
@Composable
private fun TypewriterText(
    viewModel: HomeScreenViewModel,
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontStyle: FontStyle,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    if(viewModel.displayQuoteText.value != text){
        LaunchedEffect(text) {
            text.forEachIndexed { index, _ ->
                delay(40) // Adjusted speed slightly
                viewModel.displayQuoteText.value = text.substring(0, index + 1)
            }
        }
    }


    Text(
        viewModel.displayQuoteText.value,
        style = style,
        color = color,
        fontStyle = fontStyle,
        lineHeight = lineHeight,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}