package `in`.aicortex.iso8583studio.ui.screens.hsmSimulator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── Color Constants for Key Display ─────────────────────────────────────

private val KeyBackgroundColor = Color(0xFF1A1D2E)       // Deep navy for key area
private val KeyTextColor = Color(0xFF80CBC4)              // Teal for hex values
private val KeyLabelColor = Color(0xFFB0BEC5)             // Muted label color
private val CopiedBadgeColor = Color(0xFF43A047)          // Green confirmation
private val KeyBorderColor = Color(0xFF37474F)            // Subtle border
private val LmkPairHeaderColor = Color(0xFF8E24AA)        // Security purple for LMK headers

// ─── Single Copyable Key Field ───────────────────────────────────────────

/**
 * A single key value display with:
 * - Monospace hex display
 * - One-click copy button
 * - Text selection support
 * - "Copied!" feedback animation
 * - Optional label
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CopyableKeyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    compact: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(1500)
            showCopied = false
        }
    }

    Column(modifier = modifier) {
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption.copy(
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = KeyLabelColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KeyBackgroundColor,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, KeyBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compact) 8.dp else 12.dp,
                        vertical = if (compact) 6.dp else 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Selectable key value
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value.ifEmpty { "Not Set" },
                        style = MaterialTheme.typography.body2.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (compact) 11.sp else 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (value.isNotEmpty()) KeyTextColor
                        else KeyLabelColor.copy(alpha = 0.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Copy button / Copied badge
                AnimatedContent(
                    targetState = showCopied,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.8f) + fadeIn()) with
                                (scaleOut(targetScale = 0.8f) + fadeOut())
                    },
                    label = "copy_feedback"
                ) { copied ->
                    if (copied) {
                        Surface(
                            color = CopiedBadgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = CopiedBadgeColor
                                )
                                Text(
                                    "Copied",
                                    style = MaterialTheme.typography.overline,
                                    color = CopiedBadgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (value.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(value))
                                    showCopied = true
                                }
                            },
                            modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                            enabled = value.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy $label",
                                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                                tint = if (value.isNotEmpty())
                                    KeyTextColor.copy(alpha = 0.7f)
                                else
                                    KeyLabelColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── LMK Pair Display Card ───────────────────────────────────────────────

/**
 * Displays a complete LMK key pair with:
 * - Pair number header (e.g., "LMK 00-01")
 * - Left key (K1) with copy
 * - Right key (K2) with copy
 * - Combined key copy
 * - Check Value display
 * - Usage description
 */
@Composable
fun LmkPairCard(
    pairIndex: Int,
    leftKey: String,
    rightKey: String,
    checkValue: String = "",
    usage: String = "",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showAllCopied by remember { mutableStateOf(false) }
    val leftIdx = pairIndex * 2
    val rightIdx = leftIdx + 1

    LaunchedEffect(showAllCopied) {
        if (showAllCopied) {
            delay(1500)
            showAllCopied = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LMK pair badge
                    Surface(
                        color = LmkPairHeaderColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LMK %02d-%02d".format(leftIdx, rightIdx),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = LmkPairHeaderColor
                        )
                    }

                    // Check value
                    if (checkValue.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "KCV: $checkValue",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.overline.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Copy All button
                AnimatedContent(
                    targetState = showAllCopied,
                    label = "copy_all"
                ) { copied ->
                    if (copied) {
                        Surface(
                            color = CopiedBadgeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = CopiedBadgeColor)
                                Text("All Copied", style = MaterialTheme.typography.overline, color = CopiedBadgeColor)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val combined = leftKey + rightKey
                                if (combined.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(combined))
                                    showAllCopied = true
                                }
                            },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Icon(Icons.Default.CopyAll, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy All", style = MaterialTheme.typography.overline)
                        }
                    }
                }
            }

            // Usage description
            if (usage.isNotEmpty()) {
                Text(
                    text = usage,
                    style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }

            // Key values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CopyableKeyField(
                    label = "Key %02d (Left)".format(leftIdx),
                    value = leftKey,
                    modifier = Modifier.weight(1f),
                    compact = true
                )
                CopyableKeyField(
                    label = "Key %02d (Right)".format(rightIdx),
                    value = rightKey,
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
    }
}

// ─── Copy All LMK Keys Button ────────────────────────────────────────────

/**
 * Button that copies ALL LMK keys to clipboard in a formatted text block.
 * Useful for documentation or sharing entire key sets.
 */
@Composable
fun CopyAllLmkKeysButton(
    lmkPairs: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(2000)
            showCopied = false
        }
    }

    Button(
        onClick = {
            val text = buildString {
                appendLine("═══ LMK Key Set ═══")
                appendLine()
                lmkPairs.forEachIndexed { index, (left, right) ->
                    val leftIdx = index * 2
                    val rightIdx = leftIdx + 1
                    appendLine("LMK %02d-%02d:".format(leftIdx, rightIdx))
                    appendLine("  Left  (K%02d): %s".format(leftIdx, left))
                    appendLine("  Right (K%02d): %s".format(rightIdx, right))
                    appendLine()
                }
            }
            clipboardManager.setText(AnnotatedString(text))
            showCopied = true
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (showCopied) CopiedBadgeColor
            else MaterialTheme.colors.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = if (showCopied) Icons.Default.CheckCircle else Icons.Default.CopyAll,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (showCopied) "All LMK Keys Copied!" else "Copy All LMK Keys",
            fontWeight = FontWeight.Medium
        )
    }
}