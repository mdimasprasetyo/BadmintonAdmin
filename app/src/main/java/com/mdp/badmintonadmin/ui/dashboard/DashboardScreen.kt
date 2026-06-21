package com.mdp.badmintonadmin.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdp.badmintonadmin.ui.theme.GenderFemale
import com.mdp.badmintonadmin.ui.theme.GenderMale
import com.mdp.badmintonadmin.ui.theme.OnGenderFemale
import com.mdp.badmintonadmin.ui.theme.OnGenderMale
import com.mdp.badmintonadmin.ui.components.PlayerBadge
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.domain.model.CourtAssignment
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel, 
    onManageRosterClick: () -> Unit,
    onBackToHome: () -> Unit,
    onSessionStarted: (() -> Unit)? = null,
    forceShowSetup: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.historyViewSession != null) {
        viewModel.viewHistorySession(null)
    }

    BackHandler(enabled = uiState.isActiveSessionRunning && !forceShowSetup && uiState.historyViewSession == null) {
        onBackToHome()
    }

    var showCheckInDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerMatchmakingProfile?>(null) }
    var editedName by remember { mutableStateOf("") }
    var editedGender by remember { mutableStateOf("M") }

    if (uiState.historyViewSession != null) {
        SessionHistoryDetailScreen(
            session = uiState.historyViewSession!!,
            onBack = { viewModel.viewHistorySession(null) },
            onDelete = {
                viewModel.deleteSessionHistory(uiState.historyViewSession!!)
                viewModel.viewHistorySession(null)
            }
        )
    } else if (!uiState.isActiveSessionRunning || forceShowSetup) {
        var showRestartConfirmation by remember { mutableStateOf(false) }
        var pendingCourts by remember { mutableIntStateOf(2) }
        var pendingDuration by remember { mutableIntStateOf(120) }

        if (showRestartConfirmation) {
            AlertDialog(
                onDismissRequest = { showRestartConfirmation = false },
                title = { Text("Restart Session?") },
                text = { Text("An active session is currently in progress. Starting a new session will discard the current session. Do you want to continue?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.startSession(pendingCourts, pendingDuration)
                        onSessionStarted?.invoke()
                        showRestartConfirmation = false
                    }) {
                        Text("Start New Session")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestartConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SessionSetupScreen(
            viewModel = viewModel,
            onStartClick = { courts, duration ->
                if (uiState.isActiveSessionRunning) {
                    pendingCourts = courts
                    pendingDuration = duration
                    showRestartConfirmation = true
                } else {
                    viewModel.startSession(courts, duration)
                    onSessionStarted?.invoke()
                }
            },
            onResumeClick = {
                onSessionStarted?.invoke()
            },
            onManageRosterClick = onManageRosterClick
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackToHome) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showResetConfirmation = true }) {
                            // Using error color for destructive action, ensures high contrast on app bar
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = { showSaveConfirmation = true },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("Save & End")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCheckInDialog = true },
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
                Spacer(modifier = Modifier.height(16.dp))
                // --- SECTION 1: ACTIVE COURTS ---
                Text(
                    text = "Live Courts Tracking",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.activeCourts.isEmpty()) {
                    Text(
                        text = "Waiting for enough players to fill a court...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.activeCourts, key = { it.stableId }) { court ->
                            CourtCard(
                                court = court, 
                                onFinishClick = { viewModel.completeMatch(court) },
                                availableSubstitutes = uiState.waitingLounge,
                                onSubstitute = { oldId, newProfile -> 
                                    viewModel.substitutePlayer(court, oldId, newProfile)
                                },
                                onSwapWithinCourt = { p1, p2 ->
                                    viewModel.swapPlayersWithinCourt(court.stableId, p1, p2)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- SECTION 2: THE WAITING LOUNGE ---
                Text(
                    text = "Waiting Lounge (Sorted by Priority)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.waitingLounge.isEmpty()) {
                        item { Text("Nobody is resting in the lounge.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(uiState.waitingLounge, key = { it.playerId }) { player ->
                            WaitingPlayerRow(
                                player = player, 
                                onTierChange = { newTier ->
                                    viewModel.modifyTierMidGame(player.playerId, newTier)
                                },
                                onPresenceToggle = {
                                    viewModel.togglePlayerPresence(player.playerId)
                                },
                                onLongPress = {
                                    playerToEdit = player
                                    editedName = player.name
                                    editedGender = player.gender
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (playerToEdit != null) {
        val roster by viewModel.permanentRoster.collectAsState()
        val isDuplicate = roster.any { it.id != playerToEdit!!.playerId && it.name.equals(editedName.trim(), ignoreCase = true) }

        EditPlayerDialog(
            currentName = editedName,
            currentGender = editedGender,
            isDuplicate = isDuplicate,
            onNameChange = { editedName = it },
            onGenderChange = { editedGender = it },
            onDismiss = { playerToEdit = null },
            onConfirm = {
                viewModel.updatePlayerInfo(playerToEdit!!.playerId, editedName, editedGender)
                playerToEdit = null
            }
        )
    }

    if (showCheckInDialog) {
        PlayerCheckInDialog(
            viewModel = viewModel,
            onDismiss = { showCheckInDialog = false }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Session?") },
            text = { Text("This will clear all current players and matches without saving. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSession()
                        showResetConfirmation = false
                        onBackToHome()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Save & End Session?") },
            text = { Text("This will save player stats to history and clear the board. Continue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveAndEndSession()
                    showSaveConfirmation = false
                    onBackToHome()
                }) { Text("Save & End") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCheckInDialog(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    var newPlayerName by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("B") }
    var selectedGender by remember { mutableStateOf("M") }
    var showTierDropdown by remember { mutableStateOf(false) }

    val roster by viewModel.permanentRoster.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val availableToSelect = roster.filter { player ->
        player.id !in (uiState.activeCourts.flatMap { it.getAllPlayerIds() }.toSet() +
                uiState.waitingLounge.map { it.playerId })
    }

    val trimmedName = newPlayerName.trim()
    val isNameExistsInRoster = roster.any { it.name.equals(trimmedName, ignoreCase = true) }

    val filteredSuggestions = if (trimmedName.isEmpty()) {
        availableToSelect
    } else {
        availableToSelect.filter { it.name.contains(trimmedName, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Register New Member", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isNameExistsInRoster && trimmedName.isNotEmpty()
                )

                if (isNameExistsInRoster && trimmedName.isNotEmpty()) {
                    Text(
                        text = "Name already exists in roster",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showTierDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Tier: $selectedTier")
                        }
                        DropdownMenu(expanded = showTierDropdown, onDismissRequest = { showTierDropdown = false }) {
                            listOf("S", "A", "B", "C", "D", "X").forEach { tier ->
                                DropdownMenuItem(
                                    text = { Text("Tier $tier") },
                                    onClick = { selectedTier = tier; showTierDropdown = false }
                                )
                            }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = selectedGender == "M",
                            onClick = { selectedGender = "M" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("M")
                        }
                        SegmentedButton(
                            selected = selectedGender == "W",
                            onClick = { selectedGender = "W" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("W")
                        }
                    }
                }

                Button(
                    onClick = {
                        if (trimmedName.isNotBlank()) {
                            viewModel.registerNewPlayerAndCheckIn(trimmedName, selectedTier, selectedGender)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = trimmedName.isNotEmpty() && !isNameExistsInRoster
                ) {
                    Text("Add & Check-In")
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                val listLabel = if (trimmedName.isEmpty()) "Add From Roster" else "Roster Search Suggestions"
                Text(listLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    if (filteredSuggestions.isEmpty()) {
                        val emptyMsg = if (trimmedName.isEmpty()) "No available players in roster." else "No matches found in roster."
                        item { Text(emptyMsg, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                    }
                    items(filteredSuggestions) { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerBadge(player.name, player.baseTier, player.gender)
                            OutlinedButton(onClick = {
                                viewModel.checkInPlayer(player.id, player.name, player.baseTier, player.gender)
                                onDismiss()
                            }) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CourtCard(
    court: CourtAssignment, 
    onFinishClick: () -> Unit,
    availableSubstitutes: List<PlayerMatchmakingProfile>,
    onSubstitute: (Int, PlayerMatchmakingProfile) -> Unit,
    onSwapWithinCourt: (Int, Int) -> Unit
) {
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var showEditPlayersDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COURT ${court.courtNumber}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedButton(
                    onClick = { showEditPlayersDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit Players ⚙️", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Team 1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PlayerBadge(court.team1.first.name, court.team1.first.activeTier, court.team1.first.gender)
                    Spacer(modifier = Modifier.height(4.dp))
                    PlayerBadge(court.team1.second.name, court.team1.second.activeTier, court.team1.second.gender)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Team 2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PlayerBadge(court.team2.first.name, court.team2.first.activeTier, court.team2.first.gender)
                    Spacer(modifier = Modifier.height(4.dp))
                    PlayerBadge(court.team2.second.name, court.team2.second.activeTier, court.team2.second.gender)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showFinishConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Match Finished ✔️")
            }
        }
    }

    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = { Text("Finish Match?") },
            text = { Text("Are you sure you want to finish the match on Court ${court.courtNumber}?") },
            confirmButton = {
                Button(onClick = {
                    onFinishClick()
                    showFinishConfirmation = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditPlayersDialog) {
        EditMatchPlayersDialog(
            court = court,
            availableSubstitutes = availableSubstitutes,
            onDismiss = { showEditPlayersDialog = false },
            onSubstitute = onSubstitute,
            onSwapWithinCourt = onSwapWithinCourt
        )
    }
}

@Composable
fun EditMatchPlayersDialog(
    court: CourtAssignment,
    availableSubstitutes: List<PlayerMatchmakingProfile>,
    onDismiss: () -> Unit,
    onSubstitute: (Int, PlayerMatchmakingProfile) -> Unit,
    onSwapWithinCourt: (Int, Int) -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }
    val courtPlayers = listOf(court.team1.first, court.team1.second, court.team2.first, court.team2.second)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Match Players") },
        text = {
            Column {
                Text("Tap a player to select, then tap another to swap or substitute:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("On Court:", fontWeight = FontWeight.Bold)
                courtPlayers.forEach { player ->
                    val isSelected = selectedPlayerId == player.playerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable {
                                when (selectedPlayerId) {
                                    null -> {
                                        selectedPlayerId = player.playerId
                                    }
                                    player.playerId -> {
                                        selectedPlayerId = null
                                    }
                                    else -> {
                                        onSwapWithinCourt(selectedPlayerId!!, player.playerId)
                                        onDismiss()
                                    }
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(player.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (selectedPlayerId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Substitute from lounge:", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        if (availableSubstitutes.isEmpty()) {
                            item { Text("No players available in lounge.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                        }
                        items(availableSubstitutes) { substitute ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSubstitute(selectedPlayerId!!, substitute)
                                        onDismiss()
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${substitute.name} (${substitute.activeTier})")
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Swap")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlayerDialog(
    currentName: String,
    currentGender: String,
    isDuplicate: Boolean,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val trimmedName = currentName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Player Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { onNameChange(it) },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = isDuplicate && trimmedName.isNotEmpty()
                )

                if (isDuplicate && trimmedName.isNotEmpty()) {
                    Text(
                        text = "Name already exists in roster",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Gender", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = currentGender == "M",
                        onClick = { onGenderChange("M") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GenderMale,
                            activeContentColor = OnGenderMale
                        )
                    ) {
                        Text("Man (M)")
                    }
                    SegmentedButton(
                        selected = currentGender == "W",
                        onClick = { onGenderChange("W") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GenderFemale,
                            activeContentColor = OnGenderFemale
                        )
                    ) {
                        Text("Woman (W)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                enabled = trimmedName.isNotBlank() && !isDuplicate
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WaitingPlayerRow(
    player: PlayerMatchmakingProfile, 
    onTierChange: (String) -> Unit,
    onPresenceToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    var showTierMenu by remember { mutableStateOf(false) }

    val backgroundColor = if (player.isActivePresent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val contentAlpha = if (player.isActivePresent) 1f else 0.6f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(player.name) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerBadge(player.name, player.activeTier, player.gender)
            }
            Text(
                text = "Played: ${player.gamesPlayed}x | Benched: ${player.matchesSatOutConsecutively} matches",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(
                    onClick = { showTierMenu = true },
                    enabled = player.isActivePresent,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Tier ${player.activeTier}")
                }
                DropdownMenu(expanded = showTierMenu, onDismissRequest = { showTierMenu = false }) {
                    listOf("S", "A", "B", "C", "D").forEach { tier ->
                        DropdownMenuItem(
                            text = { Text("Tier $tier") },
                            onClick = {
                                onTierChange(tier)
                                showTierMenu = false
                            }
                        )
                    }
                }
            }

            Switch(
                checked = player.isActivePresent,
                onCheckedChange = { onPresenceToggle() }
            )
        }
    }
}

@Composable
fun SessionSetupScreen(
    viewModel: DashboardViewModel,
    onStartClick: (Int, Int) -> Unit,
    onResumeClick: () -> Unit,
    onManageRosterClick: () -> Unit
) {
    var courtsText by remember { mutableStateOf("2") }
    var durationText by remember { mutableStateOf("120") }
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.sessionHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text("Badminton Admin", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isActiveSessionRunning) {
            Button(
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Resume Active Session")
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Start New Session", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = courtsText,
            onValueChange = { courtsText = it },
            label = { Text("Number of Booked Courts") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it },
            label = { Text("Session Duration (Minutes)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val courts = courtsText.toIntOrNull() ?: 2
                val duration = durationText.toIntOrNull() ?: 120
                onStartClick(courts, duration)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isActiveSessionRunning) "Restart Session" else "Open Admin Dashboard")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onManageRosterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Roster")
        }

        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Session History", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { session ->
                    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.viewHistorySession(session) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${session.courtsBooked} Courts | ${session.durationMinutes} min", fontSize = 12.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
                item {
                    var showClearHistoryConfirmation by remember { mutableStateOf(false) }

                    if (showClearHistoryConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showClearHistoryConfirmation = false },
                            title = { Text("Clear Session History?") },
                            text = { Text("Are you sure you want to delete ALL session history? This action cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.clearAllSessionHistory()
                                        showClearHistoryConfirmation = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Clear All")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearHistoryConfirmation = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextButton(
                            onClick = { showClearHistoryConfirmation = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Session History")
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryDetailScreen(
    session: SessionHistoryEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    val participants = session.participantDataJson.split("\n").filter { it.isNotBlank() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Session History?") },
            text = { Text("Are you sure you want to delete this session history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History: $date") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Session")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Courts: ${session.courtsBooked} | Duration: ${session.durationMinutes} minutes", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Final Statistics:", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(participants) { line ->
                    val parts = line.split("|")
                    if (parts.size == 3) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(parts[0], fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tier ${parts[2]}", modifier = Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Games: ${parts[1]}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
