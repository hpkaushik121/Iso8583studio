package `in`.aicortex.iso8583studio.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import `in`.aicortex.iso8583studio.ui.AccentTeal
import `in`.aicortex.iso8583studio.ui.PrimaryBlue
import `in`.aicortex.iso8583studio.ui.SuccessGreen
import `in`.aicortex.iso8583studio.ui.WarningYellow

/**
 * Information dialog explaining special field values and placeholders
 * for ISO8583 message construction
 */
@Composable
fun FieldInformationDialog(
    onCloseRequest: () -> Unit
) {
    DialogWindow(
        onCloseRequest = onCloseRequest,
        title = "Field Information - Special Values",
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            width = 650.dp,
            height = 700.dp
        ),
        resizable = true
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Information",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Field Special Values",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Dynamic placeholders for ISO8583 message fields",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Introduction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Use these special placeholders in field values to generate dynamic content. " +
                                        "The system will automatically replace these placeholders with appropriate values " +
                                        "when processing messages.",
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }

                    // [SV] Placeholder
                    SpecialValueCard(
                        placeholder = "[SV]",
                        title = "Source Value",
                        description = "Copies data from the corresponding field in the request message",
                        color = SuccessGreen,
                        icon = Icons.Default.ContentCopy,
                        examples = listOf(
                            "Field 3: [SV] → Copies Processing Code from request",
                            "Field 11: [SV] → Copies STAN from request",
                            "Field 37: [SV] → Copies Retrieval Reference Number from request"
                        ),
                        usage = "Useful for response messages where you need to echo back values from the original request."
                    )

                    // [TIME] Placeholder
                    SpecialValueCard(
                        placeholder = "[TIME]",
                        title = "Current Time",
                        description = "Generates current timestamp based on the field's expected length",
                        color = PrimaryBlue,
                        icon = Icons.Default.AccessTime,
                        examples = listOf(
                            "Field 12 (6 digits): [TIME] → 143052 (HHMMSS)",
                            "Field 13 (4 digits): [TIME] → 1204 (MMDD)",
                            "Field 7 (10 digits): [TIME] → 1204143052 (MMDDHHMISS)"
                        ),
                        usage = "Automatically formats current time according to ISO8583 field specifications."
                    )

                    // [RAND] Placeholder
                    SpecialValueCard(
                        placeholder = "[RAND]",
                        title = "Random Number",
                        description = "Generates a random numeric value based on the field's maximum length",
                        color = WarningYellow,
                        icon = Icons.Default.Casino,
                        examples = listOf(
                            "Field 11 (6 digits): [RAND] → 123456",
                            "Field 37 (12 digits): [RAND] → 987654321098",
                            "Field 38 (6 digits): [RAND] → 456789"
                        ),
                        usage = "Generates unique values for fields like STAN, RRN, or Authorization codes during testing."
                    )

                    // Usage Guidelines
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Tips",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Usage Guidelines",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GuidelineItem(
                                    text = "Placeholders are case-sensitive and must be enclosed in square brackets"
                                )

                                GuidelineItem(
                                    text = "Only one placeholder per field value is supported"
                                )

                                GuidelineItem(
                                    text = "[SV] requires a corresponding request message to copy from"
                                )

                                GuidelineItem(
                                    text = "[TIME] and [RAND] automatically pad with zeros to match field length"
                                )

                                GuidelineItem(
                                    text = "Invalid placeholders will be treated as literal text"
                                )
                            }
                        }
                    }

                    // Common Time Formats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Common Time Formats",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TimeFormatTable()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onCloseRequest,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialValueCard(
    placeholder: String,
    title: String,
    description: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    examples: List<String>,
    usage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                    )
                }

                Column {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = color
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.body1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Examples
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Examples:",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    examples.forEach { example ->
                        Text(
                            text = "• $example",
                            style = MaterialTheme.typography.body2,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Usage note
            Text(
                text = usage,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun GuidelineItem(
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.body2,
            color = AccentTeal,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun TimeFormatTable() {
    val timeFormats = listOf(
        Triple("4 digits", "MMDD", "Month and Day"),
        Triple("6 digits", "HHMMSS", "Hour, Minute, Second"),
        Triple("8 digits", "MMDDHHSS", "Month, Day, Hour, Second"),
        Triple("10 digits", "MMDDHHMISS", "Month, Day, Hour, Minute, Second"),
        Triple("12 digits", "YYMMDDHHMMSS", "Year, Month, Day, Hour, Minute, Second"),
        Triple("14 digits", "YYYYMMDDHHMMSS", "Full Year, Month, Day, Hour, Minute, Second")
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Length",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.2f)
                )
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.5f)
                )
            }

            // Rows
            timeFormats.forEach { (length, format, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = length,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = format,
                        style = MaterialTheme.typography.body2,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.weight(0.5f)
                    )
                }

                if (timeFormats.last() != Triple(length, format, description)) {
                    Divider(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}