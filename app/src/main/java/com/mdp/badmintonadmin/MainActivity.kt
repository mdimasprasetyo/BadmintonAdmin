package com.mdp.badmintonadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.mdp.badmintonadmin.ui.dashboard.DashboardScreen
import com.mdp.badmintonadmin.ui.dashboard.DashboardViewModel
import com.mdp.badmintonadmin.ui.player_mgmt.RosterScreen
import com.mdp.badmintonadmin.ui.player_mgmt.RosterViewModel
import com.mdp.badmintonadmin.ui.theme.BadmintonAdminTheme
import com.mdp.badmintonadmin.data.session.SessionStore

enum class Screen(val depth: Int) {
    Home(0), Roster(1), Dashboard(1)
}

class MainActivity : ComponentActivity() {

    // Lazy initialize the ViewModels using Android activity delegates
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val rosterViewModel: RosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge with light system bar icons over the dark court background (dark theme only).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        
        // Check if a session is already running to determine initial screen
        // Read straight from disk: a session survives the app being killed.
        val isSessionRunning = SessionStore(this).isRunning()

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

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            // Deeper screens slide in from the right; going back slides the other way (as in Pickles).
                            val dir = if (targetState.depth >= initialState.depth) 1 else -1
                            (fadeIn(tween(260, delayMillis = 40)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { dir * it / 10 }) togetherWith
                                (fadeOut(tween(160)) + slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -dir * it / 14 })
                        },
                        label = "screen"
                    ) { screen ->
                    when (screen) {
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
}
