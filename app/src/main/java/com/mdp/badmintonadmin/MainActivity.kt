package com.mdp.badmintonadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.mdp.badmintonadmin.ui.dashboard.DashboardScreen
import com.mdp.badmintonadmin.ui.dashboard.DashboardViewModel
import com.mdp.badmintonadmin.ui.player_mgmt.RosterScreen
import com.mdp.badmintonadmin.ui.player_mgmt.RosterViewModel
import com.mdp.badmintonadmin.ui.theme.BadmintonAdminTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class Screen {
    Home, Roster, Dashboard
}

class MainActivity : ComponentActivity() {

    // Lazy initialize the ViewModels using Android activity delegates
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val rosterViewModel: RosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if a session is already running to determine initial screen
        val isSessionRunning = runBlocking {
            dashboardViewModel.uiState.first().isActiveSessionRunning
        }

        setContent {
            BadmintonAdminTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { 
                        mutableStateOf(if (isSessionRunning) Screen.Dashboard else Screen.Home) 
                    }

                    // Global back handling for secondary screens
                    BackHandler(enabled = currentScreen != Screen.Home) {
                        currentScreen = Screen.Home
                    }

                    when (currentScreen) {
                        Screen.Home -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onManageRosterClick = { currentScreen = Screen.Roster },
                                onBackToHome = { /* Already at Home */ },
                                onSessionStarted = { currentScreen = Screen.Dashboard },
                                forceShowSetup = true
                            )
                        }
                        Screen.Dashboard -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onManageRosterClick = { currentScreen = Screen.Roster },
                                onBackToHome = { currentScreen = Screen.Home },
                                forceShowSetup = false
                            )
                        }
                        Screen.Roster -> {
                            RosterScreen(
                                viewModel = rosterViewModel,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}