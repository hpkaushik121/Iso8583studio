package `in`.aicortex.iso8583studio.ui.screens.config

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.GatewayConfig

/**
 * Monitor screen for viewing activity
 */
@Composable
fun MonitorScreen(
    config: GatewayConfig?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Monitoring Gateway: ${config?.name ?: "Unknown"}",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder for monitoring interface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Monitoring activity would be displayed here")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Back to Configuration")
        }
    }
}