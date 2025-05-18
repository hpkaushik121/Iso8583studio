package `in`.aicortex.iso8583studio.ui.screens.config


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Advanced Options Tab - Fifth tab in the Security Gateway configuration
 * Contains advanced configuration settings presented as a property grid
 */
@Composable
fun AdvancedOptionsTab() {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // PropertyGrid equivalent in Compose
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Advanced Options", style = MaterialTheme.typography.h6)

                // A simplified property grid implementation
                // In a real application, this would be more sophisticated
                val properties = listOf(
                    "SpecialFeature" to "None",
                    "EnableHSM" to "False",
                    "HSMPassword" to "********",
                    "LLRange" to "80-99",
                    "MaxBinaryLength" to "999",
                    "CommandTimeout" to "15s",
                    "AcquiringId" to "",
                    "UsageControl" to "False",
                    "CustomField" to ""
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, Color.Gray)
                ) {
                    properties.forEach { (name, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(8.dp)
                                .border(width = 1.dp, color = Color.LightGray, shape = MaterialTheme.shapes.small)
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f).padding(4.dp),
                                style = MaterialTheme.typography.body2
                            )

                            Text(
                                text = value,
                                modifier = Modifier.weight(1f).padding(4.dp),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
            }
        }
    }
}