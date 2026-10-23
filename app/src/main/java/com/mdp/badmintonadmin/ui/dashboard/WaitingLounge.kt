package com.mdp.badmintonadmin.ui.dashboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdp.badmintonadmin.domain.model.PlayerMatchmakingProfile
import com.mdp.badmintonadmin.ui.components.AppCard
import com.mdp.badmintonadmin.ui.components.GenderMark
import com.mdp.badmintonadmin.ui.components.RecordPill
import com.mdp.badmintonadmin.ui.components.TierBadge
import com.mdp.badmintonadmin.ui.theme.AppText
import com.mdp.badmintonadmin.ui.theme.Coral
import com.mdp.badmintonadmin.ui.theme.OnCoral

/**
 * A resting player in the lounge: queue position, name, tonight's games / bench count / record,
 * a tier badge that opens the mid-session tier menu, and the present switch.
 * Long-press the name to edit the player's name and gender.
 */
@Composable
fun WaitingPlayerRow(
    position: Int,
    player: PlayerMatchmakingProfile,
    onTierChange: (String) -> Unit,
    onPresenceToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    var showTierMenu by remember { mutableStateOf(false) }
    val contentAlpha = if (player.isActivePresent) 1f else 0.5f

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = if (player.isActivePresent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$position",
                style = AppText.NumberSmall,
                color = if (position == 1 && player.isActivePresent) Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp).alpha(contentAlpha)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
                    .pointerInput(player.name) {
                        detectTapGestures(
                            onLongPress = { onLongPress() }
                        )
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GenderMark(player.gender)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (player.sessionWins + player.sessionLosses > 0) {
                        Spacer(Modifier.width(8.dp))
                        RecordPill(player.sessionWins, player.sessionLosses)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (player.isActivePresent) {
                        "Played ${player.gamesPlayed} · Benched ${player.matchesSatOutConsecutively}"
                    } else {
                        "Away · Played ${player.gamesPlayed}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))
            Box {
                TierBadge(
                    tier = player.activeTier,
                    size = 38.dp,
                    fontSize = 21.sp,
                    enabled = player.isActivePresent,
                    onClick = { showTierMenu = true },
                    modifier = Modifier.alpha(contentAlpha)
                )
                DropdownMenu(expanded = showTierMenu, onDismissRequest = { showTierMenu = false }) {
                    listOf("S", "A", "B", "C", "D").forEach { tier ->
                        DropdownMenuItem(
                            text = { Text("Tier $tier", style = MaterialTheme.typography.labelLarge) },
                            leadingIcon = { TierBadge(tier, size = 24.dp, fontSize = 15.sp) },
                            onClick = {
                                onTierChange(tier)
                                showTierMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))
            Switch(
                checked = player.isActivePresent,
                onCheckedChange = { onPresenceToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnCoral,
                    checkedTrackColor = Coral,
                    checkedBorderColor = Coral,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
