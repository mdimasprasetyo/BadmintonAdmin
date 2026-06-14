package com.mdp.badmintonadmin.ui.player_mgmt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import com.mdp.badmintonadmin.ui.components.PlayerBadge
import com.mdp.badmintonadmin.ui.theme.GenderFemale
import com.mdp.badmintonadmin.ui.theme.GenderMale
import com.mdp.badmintonadmin.ui.theme.OnGenderFemale
import com.mdp.badmintonadmin.ui.theme.OnGenderMale
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(viewModel: RosterViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roster Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search Players") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.players, key = { it.id }) { player ->
                    PlayerRosterItem(
                        player = player,
                        onEdit = { playerToEdit = player },
                        onDelete = { playerToDelete = player }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddEditPlayerDialog(
            title = "Add New Player",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, tier, gender ->
                viewModel.addPlayer(name, tier, gender)
                showAddDialog = false
            }
        )
    }

    if (playerToEdit != null) {
        AddEditPlayerDialog(
            title = "Edit Player",
            initialName = playerToEdit!!.name,
            initialTier = playerToEdit!!.baseTier,
            initialGender = playerToEdit!!.gender,
            onDismiss = { playerToEdit = null },
            onConfirm = { name, tier, gender ->
                viewModel.updatePlayer(playerToEdit!!, name, tier, gender)
                playerToEdit = null
            }
        )
    }

    if (playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Delete Player?") },
            text = { Text("Are you sure you want to delete ${playerToDelete!!.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(playerToDelete!!)
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel")
                }
            }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PlayerBadge(player.name, player.baseTier, player.gender)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matches: ${player.totalMatchesPlayed} | Sessions: ${player.totalSessionsParticipated}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last Played: $lastPlayed",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlayerDialog(
    title: String,
    initialName: String = "",
    initialTier: String = "B",
    initialGender: String = "M",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var tier by remember { mutableStateOf(initialTier) }
    var gender by remember { mutableStateOf(initialGender) }
    var showTierDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showTierDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Tier: $tier")
                        }
                        DropdownMenu(expanded = showTierDropdown, onDismissRequest = { showTierDropdown = false }) {
                            listOf("S", "A", "B", "C", "D", "X").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("Tier $t") },
                                    onClick = { tier = t; showTierDropdown = false }
                                )
                            }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = gender == "M",
                            onClick = { gender = "M" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = GenderMale,
                                activeContentColor = OnGenderMale
                            )
                        ) {
                            Text("M")
                        }
                        SegmentedButton(
                            selected = gender == "W",
                            onClick = { gender = "W" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = GenderFemale,
                                activeContentColor = OnGenderFemale
                            )
                        ) {
                            Text("W")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, tier, gender) },
                enabled = name.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
