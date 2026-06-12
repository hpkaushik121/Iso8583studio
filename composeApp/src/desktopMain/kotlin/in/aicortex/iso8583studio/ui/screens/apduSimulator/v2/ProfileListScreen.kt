package `in`.aicortex.iso8583studio.ui.screens.apduSimulator.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.CardProfile
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.ProfileStore
import `in`.aicortex.iso8583studio.domain.service.apduSimulatorService.profile.samples.SampleProfiles

/**
 * Lists [CardProfile]s persisted in [store]. On first load, if the store is empty, it is seeded
 * with [SampleProfiles.all]. The screen exposes New / Edit / Delete / Activate actions for each
 * profile and tracks which profile is currently active (highlighted).
 */
@Composable
fun ProfileListScreen(
    store: ProfileStore,
    onEdit: (CardProfile) -> Unit,
    onNew: () -> Unit,
    onActivate: (CardProfile) -> Unit,
    activeId: String?,
) {
    val profiles = remember { mutableStateListOf<CardProfile>() }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        var loaded = store.list()
        if (loaded.isEmpty()) {
            SampleProfiles.all().forEach { store.save(it) }
            loaded = store.list()
        }
        profiles.clear()
        profiles.addAll(loaded)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Card Profiles",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New")
            }
        }

        if (profiles.isEmpty()) {
            Text(
                text = "No profiles yet.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        isActive = profile.id == activeId,
                        onActivate = { onActivate(profile) },
                        onEdit = { onEdit(profile) },
                        onDelete = {
                            store.delete(profile.id)
                            refreshTick++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: CardProfile,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val highlight = if (isActive) {
        MaterialTheme.colors.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (isActive) 4.dp else 2.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(highlight).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = MaterialTheme.colors.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = profile.name.ifBlank { profile.id },
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "${profile.scheme.name} • ${profile.applications.size} AID(s)",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onActivate) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Activate")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
