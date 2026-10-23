package com.mdp.badmintonadmin.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mdp.badmintonadmin.R
import com.mdp.badmintonadmin.domain.model.CourtAssignment
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import com.mdp.badmintonadmin.domain.session.MatchWinner
import com.mdp.badmintonadmin.ui.components.AppCard
import com.mdp.badmintonadmin.ui.components.AppTopBar
import com.mdp.badmintonadmin.ui.components.Dimens
import com.mdp.badmintonadmin.ui.components.SectionLabel
import com.mdp.badmintonadmin.ui.components.StatTile
import com.mdp.badmintonadmin.ui.components.ToolButton
import com.mdp.badmintonadmin.ui.theme.Coral
import com.mdp.badmintonadmin.ui.theme.CourtSurfaceTop
import com.mdp.badmintonadmin.ui.theme.Chalk
import com.mdp.badmintonadmin.ui.theme.OnCoral
import kotlinx.coroutines.launch

/** The live board: session stats, tool row (Undo / Reset), court cards and the waiting lounge. */
@Composable
fun SessionBoard(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    onCheckInClick: () -> Unit,
    onResetClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditPlayer: (PlayerMatchmakingProfile) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Every undoable court action confirms itself with a snackbar that can reverse it.
    fun announce(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoLastAction()
        }
    }

    fun finishMatch(court: CourtAssignment, winner: MatchWinner) {
        viewModel.completeMatch(court, winner)
        val result = when (winner) {
            MatchWinner.TEAM_1 -> " · ${court.team1.first.name} & ${court.team1.second.name} won"
            MatchWinner.TEAM_2 -> " · ${court.team2.first.name} & ${court.team2.second.name} won"
            MatchWinner.NONE -> ""
        }
        announce("Court ${court.courtNumber} finished$result")
    }

    val present = uiState.waitingLounge.count { it.isActivePresent }
    val checkedIn = uiState.waitingLounge.size + uiState.activeCourts.size * 4

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Live board",
                subtitle = "${uiState.totalCourtsBooked} courts · ${uiState.sessionDurationMinutes} min",
                onBack = onBack,
                actions = {
                    Button(
                        onClick = onSaveClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = OnCoral),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.padding(end = 8.dp).height(40.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_flag), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save & end", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCheckInClick,
                icon = { Icon(painterResource(R.drawable.ic_person_add), contentDescription = null) },
                text = { Text("Check in", style = MaterialTheme.typography.labelLarge) },
                containerColor = Coral,
                contentColor = OnCoral,
                shape = RoundedCornerShape(18.dp)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(Dimens.InnerCorner),
                    containerColor = CourtSurfaceTop,
                    contentColor = Chalk,
                    actionColor = Coral
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 4.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        value = "${uiState.activeCourts.size}/${uiState.totalCourtsBooked}",
                        label = "Courts live",
                        valueColor = Coral,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(value = "$present", label = "Waiting", modifier = Modifier.weight(1f))
                    StatTile(value = "$checkedIn", label = "Checked in", modifier = Modifier.weight(1f))
                }
            }

            item(key = "tools") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ToolButton(
                            text = "Undo",
                            icon = painterResource(R.drawable.ic_undo),
                            onClick = {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                viewModel.undoLastAction()
                            },
                            enabled = uiState.undoLabel != null,
                            modifier = Modifier.weight(1f)
                        )
                        ToolButton(
                            text = "Reset",
                            icon = painterResource(R.drawable.ic_reset),
                            onClick = onResetClick,
                            contentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (uiState.undoLabel != null) {
                        Text(
                            text = "Undo: ${uiState.undoLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                }
            }

            item(key = "courts-label") {
                SectionLabel("Courts", modifier = Modifier.padding(top = 8.dp))
            }

            if (uiState.activeCourts.isEmpty()) {
                item(key = "courts-empty") {
                    EmptyNote("Waiting for enough players to fill a court…")
                }
            } else {
                items(uiState.activeCourts, key = { "court-${it.stableId}" }) { court ->
                    CourtCard(
                        court = court,
                        availableSubstitutes = uiState.waitingLounge,
                        onFinish = { winner -> finishMatch(court, winner) },
                        onSubstitute = { oldId, newProfile ->
                            val outgoing = court.getAllPlayerIds().indexOf(oldId).let { index ->
                                listOf(court.team1.first, court.team1.second, court.team2.first, court.team2.second)
                                    .getOrNull(index)?.name
                            } ?: "Player"
                            viewModel.substitutePlayer(court, oldId, newProfile)
                            announce("${newProfile.name} in for $outgoing on Court ${court.courtNumber}")
                        },
                        onSwapWithinCourt = { p1, p2 ->
                            viewModel.swapPlayersWithinCourt(court.stableId, p1, p2)
                            announce("Players swapped on Court ${court.courtNumber}")
                        }
                    )
                }
            }

            item(key = "lounge-label") {
                SectionLabel("Waiting lounge", modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "BY PRIORITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.waitingLounge.isEmpty()) {
                item(key = "lounge-empty") {
                    EmptyNote("Nobody is resting in the lounge.")
                }
            } else {
                itemsIndexed(uiState.waitingLounge, key = { _, player -> "player-${player.playerId}" }) { index, player ->
                    WaitingPlayerRow(
                        position = index + 1,
                        player = player,
                        onTierChange = { newTier ->
                            viewModel.modifyTierMidGame(player.playerId, newTier)
                        },
                        onPresenceToggle = {
                            viewModel.togglePlayerPresence(player.playerId)
                        },
                        onLongPress = { onEditPlayer(player) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp).align(Alignment.CenterHorizontally)
        )
    }
}
