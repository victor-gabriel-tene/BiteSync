package org.bytestorm.bitesync

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bytestorm.bitesync.ui.screen.LobbyScreen
import org.bytestorm.bitesync.ui.screen.MatchScreen
import org.bytestorm.bitesync.ui.screen.SuggestScreen
import org.bytestorm.bitesync.ui.screen.SwipeScreen
import org.bytestorm.bitesync.viewmodel.AppScreen
import org.bytestorm.bitesync.viewmodel.BiteSyncViewModel

@Composable
fun App() {
    MaterialTheme {
        var serverIp by remember { mutableStateOf("10.0.2.2") }
        val viewModel = viewModel(key = serverIp) { BiteSyncViewModel("http://$serverIp:8080") }

        val screen by viewModel.screen.collectAsState()
        val roomState by viewModel.roomState.collectAsState()
        val venues by viewModel.venues.collectAsState()
        val currentIndex by viewModel.currentCardIndex.collectAsState()
        val predictions by viewModel.predictions.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()
        val error by viewModel.error.collectAsState()
        val isConnecting by viewModel.isConnecting.collectAsState()

        when (val currentScreen = screen) {
            is AppScreen.Lobby -> {
                LobbyScreen(
                    roomState = roomState,
                    isConnecting = isConnecting,
                    error = error,
                    serverIp = serverIp,
                    onServerIpChange = { serverIp = it },
                    onCreateRoom = { name -> viewModel.createRoom(name) },
                    onJoinRoom = { pin, name -> viewModel.joinRoom(pin, name) },
                    onStartSuggesting = { viewModel.startSuggesting() },
                    onClearError = { viewModel.clearError() }
                )
            }

            is AppScreen.Suggesting -> {
                SuggestScreen(
                    roomState = roomState,
                    predictions = predictions,
                    isSearching = isSearching,
                    error = error,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onPredictionSelected = { viewModel.onPredictionSelected(it) },
                    onStartSwiping = { viewModel.startSwiping() },
                    onClearError = { viewModel.clearError() }
                )
            }

            is AppScreen.Swiping -> {
                SwipeScreen(
                    venues = venues,
                    currentIndex = currentIndex,
                    roomState = roomState,
                    onSwipe = { venueId, liked -> viewModel.onSwipe(venueId, liked) }
                )
            }

            is AppScreen.Match -> {
                MatchScreen(
                    venue = currentScreen.venue,
                    onBackToLobby = { viewModel.returnToLobby() }
                )
            }
        }
    }
}
