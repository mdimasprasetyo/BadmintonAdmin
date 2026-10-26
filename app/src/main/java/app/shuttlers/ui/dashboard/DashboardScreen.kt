package app.shuttlers.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.shuttlers.domain.model.PlayerMatchmakingProfile
import app.shuttlers.ui.components.ConfirmDialog

/**
 * Entry point for the session flow. Routes between the setup screen (with resume and history),
 * a past session's details, and the live board, and owns the session-wide dialogs.
 * The pieces live in SessionSetupScreen.kt, SessionBoard.kt, CourtCard.kt, WaitingLounge.kt,
 * SessionHistoryScreen.kt and DashboardDialogs.kt.
 */
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

    val pane = when {
        uiState.historyViewSession != null -> Pane.History
        !uiState.isActiveSessionRunning || forceShowSetup -> Pane.Setup
        else -> Pane.Board
    }
    // Last non-null history entry, so the detail screen can still draw while it animates out.
    var lastHistory by remember { mutableStateOf(uiState.historyViewSession) }
    if (uiState.historyViewSession != null) lastHistory = uiState.historyViewSession

    AnimatedContent(
        targetState = pane,
        transitionSpec = {
            (fadeIn(tween(240, delayMillis = 40)) + scaleIn(tween(280), initialScale = 0.97f)) togetherWith fadeOut(tween(150))
        },
        label = "pane"
    ) { target ->
    if (target == Pane.History && lastHistory != null) {
        SessionHistoryDetailScreen(
            session = lastHistory!!,
            onBack = { viewModel.viewHistorySession(null) },
            onDelete = {
                viewModel.deleteSessionHistory(lastHistory!!)
                viewModel.viewHistorySession(null)
            }
        )
    } else if (target == Pane.Setup || target == Pane.History) {
        var showRestartConfirmation by remember { mutableStateOf(false) }
        var pendingCourts by remember { mutableIntStateOf(2) }
        var pendingDuration by remember { mutableIntStateOf(120) }

        if (showRestartConfirmation) {
            ConfirmDialog(
                title = "Restart session?",
                message = "A session is in progress. Starting a new one discards the current session. Continue?",
                confirmText = "Start new session",
                destructive = true,
                onConfirm = {
                    viewModel.startSession(pendingCourts, pendingDuration)
                    onSessionStarted?.invoke()
                    showRestartConfirmation = false
                },
                onDismiss = { showRestartConfirmation = false }
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
        SessionBoard(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome,
            onCheckInClick = { showCheckInDialog = true },
            onResetClick = { showResetConfirmation = true },
            onSaveClick = { showSaveConfirmation = true },
            onEditPlayer = { player ->
                playerToEdit = player
                editedName = player.name
                editedGender = player.gender
            }
        )
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
        ConfirmDialog(
            title = "Reset session?",
            message = "This clears all checked-in players and matches without saving. Continue?",
            confirmText = "Reset",
            destructive = true,
            onConfirm = {
                viewModel.resetSession()
                showResetConfirmation = false
                onBackToHome()
            },
            onDismiss = { showResetConfirmation = false }
        )
    }

    if (showSaveConfirmation) {
        ConfirmDialog(
            title = "Save & end session?",
            message = "Player stats, wins and losses are saved to history and the roster, and the board is cleared. Matches still on court won't be counted. Continue?",
            confirmText = "Save & end",
            onConfirm = {
                viewModel.saveAndEndSession()
                showSaveConfirmation = false
                onBackToHome()
            },
            onDismiss = { showSaveConfirmation = false }
        )
    }
}

private enum class Pane { Setup, Board, History }
