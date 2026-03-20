package `in`.aicortex.iso8583studio.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Global license-check state for UI-level enforcement.
 *
 * Screens call [checkAndRun] before opening a tool or starting a simulator.
 * - VALID / EXPIRING_SOON → runs the action immediately
 * - TRIAL → shows an informational dialog; user can Continue (runs action) or Cancel
 * - Anything else → shows a blocking dialog; action is not run
 */
object LicenseCheck {

    var blockMessage = mutableStateOf<String?>(null)
        private set

    var trialInfo = mutableStateOf<String?>(null)
        private set

    private var pendingAction: (() -> Unit)? = null

    fun checkAndRun(action: () -> Unit): Boolean {
        val msg = LicenseGate.blockingMessage()
        if (msg != null) {
            blockMessage.value = msg
            return false
        }

        val snapshot = LicenseService.currentSnapshot
        if (snapshot.state == LicenseState.TRIAL) {
            val days = snapshot.daysUntilExpiry
            val daysText = if (days > 0) "$days day${if (days != 1) "s" else ""} remaining." else "Trial period active."
            trialInfo.value = "You are using ISO8583 Studio in trial mode.\n$daysText\n\nActivate a license for uninterrupted access."
            pendingAction = action
            return false
        }

        action()
        return true
    }

    fun continueFromTrial() {
        trialInfo.value = null
        pendingAction?.invoke()
        pendingAction = null
    }

    fun dismissTrial() {
        trialInfo.value = null
        pendingAction = null
    }

    fun dismiss() {
        blockMessage.value = null
    }
}

/**
 * Place this once in the main window composition.
 * Renders either the trial info dialog or the blocking dialog.
 */
@Composable
fun LicenseBlockDialog() {
    val blockMsg by LicenseCheck.blockMessage
    val trialMsg by LicenseCheck.trialInfo

    if (trialMsg != null) {
        AlertDialog(
            onDismissRequest = { LicenseCheck.dismissTrial() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Trial Mode", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(trialMsg ?: "")
            },
            confirmButton = {
                Button(onClick = { LicenseCheck.continueFromTrial() }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { LicenseCheck.dismissTrial() }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.surface
        )
    }

    if (blockMsg != null) {
        AlertDialog(
            onDismissRequest = { LicenseCheck.dismiss() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("License Required", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(blockMsg ?: "")
            },
            confirmButton = {
                Button(onClick = { LicenseCheck.dismiss() }) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.surface
        )
    }
}
