package com.mdp.badmintonadmin.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.mdp.badmintonadmin.R
import com.mdp.badmintonadmin.ui.components.Dimens
import com.mdp.badmintonadmin.ui.components.GenderSelector
import com.mdp.badmintonadmin.ui.components.PlayerBadge
import com.mdp.badmintonadmin.ui.components.PrimaryButton
import com.mdp.badmintonadmin.ui.components.QuietTextButton
import com.mdp.badmintonadmin.ui.components.SectionLabel
import com.mdp.badmintonadmin.ui.components.TierSelector
import com.mdp.badmintonadmin.ui.theme.Coral
import com.mdp.badmintonadmin.ui.theme.OnCoral

/** Check a player in: register a brand-new member, or pick someone from the club roster. */
@Composable
fun PlayerCheckInDialog(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    var newPlayerName by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("B") }
    var selectedGender by remember { mutableStateOf("M") }

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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Check in player", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                SectionLabel("New member")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Player name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.InnerCorner),
                    isError = isNameExistsInRoster && trimmedName.isNotEmpty(),
                    supportingText = if (isNameExistsInRoster && trimmedName.isNotEmpty()) {
                        { Text("Name already exists in the roster") }
                    } else null
                )

                Spacer(Modifier.height(12.dp))
                SectionLabel("Tier")
                Spacer(Modifier.height(6.dp))
                TierSelector(selected = selectedTier, onSelect = { selectedTier = it })

                Spacer(Modifier.height(12.dp))
                SectionLabel("Gender")
                Spacer(Modifier.height(6.dp))
                GenderSelector(selected = selectedGender, onSelect = { selectedGender = it })

                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "Add & check in",
                    icon = painterResource(R.drawable.ic_person_add),
                    onClick = {
                        if (trimmedName.isNotBlank()) {
                            viewModel.registerNewPlayerAndCheckIn(trimmedName, selectedTier, selectedGender)
                            onDismiss()
                        }
                    },
                    enabled = trimmedName.isNotEmpty() && !isNameExistsInRoster,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                val listLabel = if (trimmedName.isEmpty()) "From the roster" else "Roster matches"
                SectionLabel(listLabel)
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredSuggestions.isEmpty()) {
                        val emptyMsg = if (trimmedName.isEmpty()) "No available players in the roster." else "No matches in the roster."
                        item {
                            Text(
                                emptyMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                    items(filteredSuggestions, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerBadge(player.name, player.baseTier, player.gender, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.weight(1f).width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.checkInPlayer(player.id, player.name, player.baseTier, player.gender)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Coral.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { QuietTextButton("Close", onDismiss) }
    )
}

/** Edit a checked-in player's name and gender (long-press in the lounge). */
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Edit player", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { onNameChange(it) },
                    label = { Text("Player name") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.InnerCorner),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isDuplicate && trimmedName.isNotEmpty(),
                    supportingText = if (isDuplicate && trimmedName.isNotEmpty()) {
                        { Text("Name already exists in the roster") }
                    } else null
                )

                Spacer(Modifier.height(12.dp))
                SectionLabel("Gender")
                Spacer(Modifier.height(6.dp))
                GenderSelector(selected = currentGender, onSelect = onGenderChange)
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm() },
                enabled = trimmedName.isNotBlank() && !isDuplicate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = OnCoral)
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { QuietTextButton("Cancel", onDismiss) }
    )
}
