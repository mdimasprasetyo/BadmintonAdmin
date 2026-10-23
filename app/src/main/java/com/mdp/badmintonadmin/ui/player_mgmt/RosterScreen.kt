package com.mdp.badmintonadmin.ui.player_mgmt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdp.badmintonadmin.R
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import com.mdp.badmintonadmin.ui.components.AppCard
import com.mdp.badmintonadmin.ui.components.AppTopBar
import com.mdp.badmintonadmin.ui.components.ConfirmDialog
import com.mdp.badmintonadmin.ui.components.Dimens
import com.mdp.badmintonadmin.ui.components.GenderMark
import com.mdp.badmintonadmin.ui.components.GenderSelector
import com.mdp.badmintonadmin.ui.components.QuietTextButton
import com.mdp.badmintonadmin.ui.components.RecordPill
import com.mdp.badmintonadmin.ui.components.SectionLabel
import com.mdp.badmintonadmin.ui.components.TierBadge
import com.mdp.badmintonadmin.ui.components.TierSelector
import com.mdp.badmintonadmin.ui.theme.AppText
import com.mdp.badmintonadmin.ui.theme.Coral
import com.mdp.badmintonadmin.ui.theme.OnCoral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RosterScreen(viewModel: RosterViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Roster",
                subtitle = "${uiState.players.size} players",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(painterResource(R.drawable.ic_person_add), contentDescription = null) },
                text = { Text("Add player", style = MaterialTheme.typography.labelLarge) },
                containerColor = Coral,
                contentColor = OnCoral,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search players") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(Dimens.InnerCorner),
                    singleLine = true
                )
            }

            if (uiState.players.isEmpty()) {
                item(key = "empty") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (uiState.searchQuery.isBlank()) "No players yet. Add your club members to get started." else "No players match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }
                }
            }

            items(uiState.players, key = { it.id }) { player ->
                PlayerRosterItem(
                    player = player,
                    onEdit = { playerToEdit = player },
                    onDelete = { playerToDelete = player }
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditPlayerDialog(
            title = "Add player",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, tier, gender ->
                viewModel.addPlayer(name, tier, gender)
                showAddDialog = false
            }
        )
    }

    if (playerToEdit != null) {
        AddEditPlayerDialog(
            title = "Edit player",
            initialName = playerToEdit!!.name,
            initialTier = playerToEdit!!.baseTier,
            initialGender = playerToEdit!!.gender,
            player = playerToEdit,
            onDismiss = { playerToEdit = null },
            onConfirm = { name, tier, gender ->
                viewModel.updatePlayer(playerToEdit!!, name, tier, gender)
                playerToEdit = null
            }
        )
    }

    if (playerToDelete != null) {
        ConfirmDialog(
            title = "Delete player?",
            message = "Delete ${playerToDelete!!.name} from the roster? This cannot be undone.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deletePlayer(playerToDelete!!)
                playerToDelete = null
            },
            onDismiss = { playerToDelete = null }
        )
    }
}

@Composable
fun PlayerRosterItem(
    player: PlayerEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lastPlayed = player.lastPlayedTimestamp?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Never"

    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TierBadge(player.baseTier, size = 40.dp, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GenderMark(player.gender, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    RecordPill(player.totalWins, player.totalLosses)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${player.totalMatchesPlayed} matches · ${player.totalSessionsParticipated} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last played $lastPlayed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEditPlayerDialog(
    title: String,
    initialName: String = "",
    initialTier: String = "B",
    initialGender: String = "M",
    player: PlayerEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var tier by remember { mutableStateOf(initialTier) }
    var gender by remember { mutableStateOf(initialGender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                if (player != null) {
                    PlayerRecordSummary(player)
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.InnerCorner),
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                SectionLabel("Tier")
                Spacer(Modifier.height(6.dp))
                TierSelector(selected = tier, onSelect = { tier = it })
                Spacer(Modifier.height(14.dp))
                SectionLabel("Gender")
                Spacer(Modifier.height(6.dp))
                GenderSelector(selected = gender, onSelect = { gender = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, tier, gender) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = OnCoral)
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { QuietTextButton("Cancel", onDismiss) }
    )
}

/** Lifetime numbers for one player: wins, losses, matches, sessions. */
@Composable
private fun PlayerRecordSummary(player: PlayerEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "${player.totalWins}" to "Wins",
            "${player.totalLosses}" to "Losses",
            "${player.totalMatchesPlayed}" to "Matches",
            "${player.totalSessionsParticipated}" to "Sessions"
        ).forEachIndexed { index, (value, label) ->
            AppCard(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(value, style = AppText.NumberSmall, color = if (index == 0) Coral else MaterialTheme.colorScheme.onSurface)
                    Text(label.uppercase(), style = AppText.SectionLabel.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}
