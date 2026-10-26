package app.shuttlers.ui.dashboard

import app.shuttlers.ui.components.Motion
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shuttlers.R
import app.shuttlers.domain.model.CourtAssignment
import app.shuttlers.domain.model.PlayerMatchmakingProfile
import app.shuttlers.domain.session.MatchWinner
import app.shuttlers.ui.components.AppCard
import app.shuttlers.ui.components.Dimens
import app.shuttlers.ui.components.OutlineToolButton
import app.shuttlers.ui.components.PlayerBadge
import app.shuttlers.ui.components.PlayerLine
import app.shuttlers.ui.components.PrimaryButton
import app.shuttlers.ui.components.QuietTextButton
import app.shuttlers.ui.components.SectionLabel
import app.shuttlers.ui.theme.AppText
import app.shuttlers.ui.theme.Coral
import app.shuttlers.ui.theme.CourtGreen

/** One court in play: court number, Team 1 vs Team 2, and the finish / edit-players actions. */
@Composable
fun CourtCard(
    court: CourtAssignment,
    modifier: Modifier = Modifier,
    availableSubstitutes: List<PlayerMatchmakingProfile>,
    onFinish: (MatchWinner) -> Unit,
    onSubstitute: (Int, PlayerMatchmakingProfile) -> Unit,
    onSwapWithinCourt: (Int, Int) -> Unit
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showEditPlayersDialog by remember { mutableStateOf(false) }

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COURT",
                        style = AppText.CondensedLabel.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${court.courtNumber}",
                        style = AppText.NumberLarge,
                        color = Coral,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                OutlineToolButton(
                    text = "Edit players",
                    icon = painterResource(R.drawable.ic_swap),
                    onClick = { showEditPlayersDialog = true },
                    height = 40.dp
                )
            }

            Spacer(Modifier.height(12.dp))
            TeamBlock(label = "Team 1", team = court.team1)
            VsDivider()
            TeamBlock(label = "Team 2", team = court.team2)
            Spacer(Modifier.height(14.dp))

            PrimaryButton(
                text = "Finish match",
                icon = painterResource(R.drawable.ic_flag),
                onClick = { showFinishDialog = true },
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp
            )
        }
    }

    if (showFinishDialog) {
        FinishMatchDialog(
            court = court,
            onResult = { winner ->
                showFinishDialog = false
                onFinish(winner)
            },
            onDismiss = { showFinishDialog = false }
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
private fun TeamBlock(label: String, team: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>) {
    Surface(
        shape = RoundedCornerShape(Dimens.InnerCorner),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = AppText.CondensedLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            CourtPlayer(team.first)
            Spacer(Modifier.height(8.dp))
            CourtPlayer(team.second)
        }
    }
}

/** A player slot that crossfades when someone is substituted or swapped in. */
@Composable
private fun CourtPlayer(player: PlayerMatchmakingProfile) {
    AnimatedContent(
        targetState = player,
        contentKey = { it.playerId },
        transitionSpec = {
            (fadeIn(tween(Motion.MEDIUM)) + slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.emphasized)) { it / 12 }) togetherWith
                fadeOut(tween(Motion.SHORT))
        },
        label = "courtPlayer"
    ) { p ->
        PlayerLine(p.name, p.activeTier, p.gender, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VsDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("VS", style = AppText.CondensedLabel.copy(fontSize = 15.sp), color = Coral)
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Asks which pair won; "No result" finishes the match without crediting a win or a loss. */
@Composable
fun FinishMatchDialog(
    court: CourtAssignment,
    onResult: (MatchWinner) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Who won on Court ${court.courtNumber}?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tap the winning pair. Wins and losses go on each player's record when the session is saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WinnerOption("Team 1", court.team1) { onResult(MatchWinner.TEAM_1) }
                WinnerOption("Team 2", court.team2) { onResult(MatchWinner.TEAM_2) }
            }
        },
        confirmButton = {
            QuietTextButton("No result", onClick = { onResult(MatchWinner.NONE) }, color = MaterialTheme.colorScheme.onSurface)
        },
        dismissButton = { QuietTextButton("Cancel", onDismiss) }
    )
}

@Composable
private fun WinnerOption(
    label: String,
    team: Pair<PlayerMatchmakingProfile, PlayerMatchmakingProfile>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_trophy),
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label.uppercase(), style = AppText.CondensedLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${team.first.name} & ${team.second.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Tap a player to select, then tap another on court to swap, or one from the lounge to substitute. */
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Edit Court ${court.courtNumber}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Tap a player to select, then tap another to swap or substitute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                SectionLabel("On court")
                Spacer(Modifier.height(4.dp))
                courtPlayers.forEachIndexed { index, player ->
                    val isSelected = selectedPlayerId == player.playerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CourtGreen else Color.Transparent)
                            .clickable {
                                when (selectedPlayerId) {
                                    null -> selectedPlayerId = player.playerId
                                    player.playerId -> selectedPlayerId = null
                                    else -> {
                                        onSwapWithinCourt(selectedPlayerId!!, player.playerId)
                                        onDismiss()
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Coral)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (index < 2) "T1" else "T2",
                            style = AppText.CondensedLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        PlayerLine(player.name, player.activeTier, player.gender, modifier = Modifier.weight(1f))
                    }
                }

                if (selectedPlayerId != null) {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Substitute from lounge")
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        if (availableSubstitutes.isEmpty()) {
                            item {
                                Text(
                                    "No players available in the lounge.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(availableSubstitutes) { substitute ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSubstitute(selectedPlayerId!!, substitute)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerBadge(substitute.name, substitute.activeTier, substitute.gender, modifier = Modifier.weight(1f, fill = false))
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    painterResource(R.drawable.ic_swap),
                                    contentDescription = "Substitute",
                                    tint = Coral
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { QuietTextButton("Close", onDismiss) }
    )
}
