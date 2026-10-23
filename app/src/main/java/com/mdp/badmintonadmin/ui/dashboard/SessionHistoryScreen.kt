package com.mdp.badmintonadmin.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.domain.session.SessionRules
import com.mdp.badmintonadmin.ui.components.AppCard
import com.mdp.badmintonadmin.ui.components.AppTopBar
import com.mdp.badmintonadmin.ui.components.ConfirmDialog
import com.mdp.badmintonadmin.ui.components.Dimens
import com.mdp.badmintonadmin.ui.components.RecordPill
import com.mdp.badmintonadmin.ui.components.SectionLabel
import com.mdp.badmintonadmin.ui.components.StatTile
import com.mdp.badmintonadmin.ui.components.TierBadge
import com.mdp.badmintonadmin.ui.theme.AppText
import com.mdp.badmintonadmin.ui.theme.Coral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A past session: summary tiles and each player's final games, tier and (from v1.2) record. */
@Composable
fun SessionHistoryDetailScreen(
    session: SessionHistoryEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    val participants = session.participantDataJson.split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { SessionRules.decodeHistoryLine(it) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        ConfirmDialog(
            title = "Delete this session?",
            message = "This session's history will be deleted. It cannot be undone.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                onDelete()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Session",
                subtitle = date,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete session", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "summary") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("${session.courtsBooked}", "Courts", modifier = Modifier.weight(1f), valueColor = Coral)
                    StatTile("${session.durationMinutes}", "Minutes", modifier = Modifier.weight(1f))
                    StatTile("${participants.size}", "Players", modifier = Modifier.weight(1f))
                }
            }
            item(key = "label") {
                SectionLabel("Final statistics", modifier = Modifier.padding(top = 10.dp))
            }
            items(participants) { line ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TierBadge(line.tier)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            line.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (line.wins != null && line.losses != null) {
                            Spacer(Modifier.width(8.dp))
                            RecordPill(line.wins, line.losses)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(line.games, style = AppText.NumberSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("GAMES", style = AppText.SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
