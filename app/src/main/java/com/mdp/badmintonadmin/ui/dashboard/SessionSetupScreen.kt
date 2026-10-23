package com.mdp.badmintonadmin.ui.dashboard

import com.mdp.badmintonadmin.ui.components.enter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mdp.badmintonadmin.R
import com.mdp.badmintonadmin.data.local.entity.SessionHistoryEntity
import com.mdp.badmintonadmin.ui.components.AppCard
import com.mdp.badmintonadmin.ui.components.ConfirmDialog
import com.mdp.badmintonadmin.ui.components.Dimens
import com.mdp.badmintonadmin.ui.components.PrimaryButton
import com.mdp.badmintonadmin.ui.components.QuietTextButton
import com.mdp.badmintonadmin.ui.components.SectionLabel
import com.mdp.badmintonadmin.ui.components.ToolButton
import com.mdp.badmintonadmin.ui.theme.AppText
import com.mdp.badmintonadmin.ui.theme.Coral
import com.mdp.badmintonadmin.ui.theme.CourtGreen
import com.mdp.badmintonadmin.ui.theme.OnCourtGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home: resume a running session, start a new one, open the roster, browse past sessions. */
@Composable
fun SessionSetupScreen(
    viewModel: DashboardViewModel,
    onStartClick: (Int, Int) -> Unit,
    onResumeClick: () -> Unit,
    onManageRosterClick: () -> Unit
) {
    var courtsText by remember { mutableStateOf("2") }
    var durationText by remember { mutableStateOf("120") }
    var showClearHistoryConfirmation by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.sessionHistory.collectAsState()

    val courts = courtsText.toIntOrNull()
    val duration = durationText.toIntOrNull()
    val courtsValid = courts != null && courts in SessionLimits.COURTS
    val durationValid = duration != null && duration in SessionLimits.DURATION_MINUTES

    if (showClearHistoryConfirmation) {
        ConfirmDialog(
            title = "Clear session history?",
            message = "This deletes ALL past sessions. It cannot be undone.",
            confirmText = "Clear all",
            destructive = true,
            onConfirm = {
                viewModel.clearAllSessionHistory()
                showClearHistoryConfirmation = false
            },
            onDismiss = { showClearHistoryConfirmation = false }
        )
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(modifier = Modifier.enter(0).padding(top = 12.dp, bottom = 8.dp)) {
                    Text("BADMINTON ADMIN", style = AppText.TitleCaps, color = Coral)
                    Spacer(Modifier.height(10.dp))
                    Text("Run tonight's courts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Check players in as they arrive and every court gets a fair, balanced doubles match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.isActiveSessionRunning) {
                item(key = "resume") {
                    AppCard(color = CourtGreen, outlined = false, modifier = Modifier.animateItem().enter(1).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(Dimens.CardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SESSION IN PROGRESS", style = AppText.SectionLabel, color = OnCourtGreen.copy(alpha = 0.8f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${uiState.totalCourtsBooked} courts · ${uiState.sessionDurationMinutes} min",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnCourtGreen
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            PrimaryButton(
                                text = "Resume",
                                icon = rememberVectorPainter(Icons.Filled.PlayArrow),
                                onClick = onResumeClick,
                                height = 48.dp
                            )
                        }
                    }
                }
            }

            item(key = "setup") {
                AppCard(modifier = Modifier.animateItem().enter(2).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionLabel(if (uiState.isActiveSessionRunning) "Start a new session" else "New session")
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = courtsText,
                                onValueChange = { input -> courtsText = input.filter { it.isDigit() }.take(2) },
                                label = { Text("Booked courts") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_court), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                isError = !courtsValid,
                                supportingText = if (!courtsValid) {
                                    { Text("${SessionLimits.COURTS.first}–${SessionLimits.COURTS.last} courts") }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = AppText.NumberSmall,
                                shape = RoundedCornerShape(Dimens.InnerCorner),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { input -> durationText = input.filter { it.isDigit() }.take(3) },
                                label = { Text("Minutes") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                isError = !durationValid,
                                supportingText = if (!durationValid) {
                                    { Text("${SessionLimits.DURATION_MINUTES.first}–${SessionLimits.DURATION_MINUTES.last} min") }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = AppText.NumberSmall,
                                shape = RoundedCornerShape(Dimens.InnerCorner),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = if (uiState.isActiveSessionRunning) "Restart session" else "Open admin dashboard",
                            onClick = { onStartClick(courts!!, duration!!) },
                            enabled = courtsValid && durationValid,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item(key = "roster") {
                ToolButton(
                    text = "Manage roster",
                    icon = painterResource(R.drawable.ic_groups),
                    onClick = onManageRosterClick,
                    modifier = Modifier.animateItem().enter(3).fillMaxWidth(),
                    height = 52.dp
                )
            }

            if (history.isNotEmpty()) {
                item(key = "history-label") {
                    SectionLabel("Session history", modifier = Modifier.padding(top = 16.dp))
                }
                items(history, key = { "history-${it.sessionId}" }) { session ->
                    HistoryRow(session = session, onClick = { viewModel.viewHistorySession(session) }, modifier = Modifier.animateItem())
                }
                item(key = "history-clear") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuietTextButton(
                            "Clear session history",
                            onClick = { showClearHistoryConfirmation = true },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: SessionHistoryEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(date, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${session.courtsBooked} courts · ${session.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
