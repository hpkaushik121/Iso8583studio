package `in`.aicortex.iso8583studio.ui.screens.config.hostSimulator


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.data.model.CipherMode
import `in`.aicortex.iso8583studio.data.model.CipherType
import `in`.aicortex.iso8583studio.data.model.SecurityKey
import kotlin.random.Random

/**
 * Keys Setting Tab - Third tab in the Security Gateway configuration
 * Manages encryption/decryption keys
 */
@Composable
fun KeysSettingTab(keysList: List<SecurityKey>, onKeysListChange: (List<SecurityKey>) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // Encryption/Decryption Option group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Encryption/Decryption Option", style = MaterialTheme.typography.subtitle1)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = true, // Default is enabled
                        onCheckedChange = { /* Update encryption enabled state */ }
                    )
                    Text("Enable encrypt/decrypt")

                    Spacer(modifier = Modifier.width(16.dp))

                    Text("Chosen setting", modifier = Modifier.padding(horizontal = 8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text(if (keysList.isNotEmpty()) keysList[0].name else "Select key")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            keysList.forEach { key ->
                                DropdownMenuItem(onClick = {
                                    // Logic to select key
                                    expanded = false
                                }) {
                                    Text(key.name)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Available keys group
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 300.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Available keys", style = MaterialTheme.typography.subtitle1)

                // Keys table
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                        .border(1.dp, Color.Gray)
                        .padding(4.dp)
                ) {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.LightGray),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Name", modifier = Modifier.weight(1f).padding(4.dp), style = MaterialTheme.typography.subtitle2)
                        Text("Cipher Type", modifier = Modifier.weight(1f).padding(4.dp), style = MaterialTheme.typography.subtitle2)
                        Text("Cipher Mode", modifier = Modifier.weight(1f).padding(4.dp), style = MaterialTheme.typography.subtitle2)
                        Text("Description", modifier = Modifier.weight(1f).padding(4.dp), style = MaterialTheme.typography.subtitle2)
                    }

                    // Table rows
                    keysList.forEach { key ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .border(1.dp, Color.LightGray)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(key.name, modifier = Modifier.weight(1f))
                            Text(key.cipherType.toString(), modifier = Modifier.weight(1f))
                            Text(key.cipherMode.toString(), modifier = Modifier.weight(1f))
                            Text(key.description, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Key management buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Add new key logic
                            val newKey = SecurityKey(
                                id = Random.nextInt().toString(),
                                name = "NewKey${keysList.size + 1}",
                                cipherType = CipherType.AES_256,
                                cipherMode = CipherMode.CBC.ordinal,
                                description = "New key"
                            )
                            onKeysListChange(keysList + newKey)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add New")
                    }

                    Button(
                        onClick = { /* Modify key logic */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Modify")
                    }

                    Button(
                        onClick = {
                            // Delete key logic - would need selectedKeyIndex
                            if (keysList.isNotEmpty()) {
                                onKeysListChange(keysList.subList(1, keysList.size))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}