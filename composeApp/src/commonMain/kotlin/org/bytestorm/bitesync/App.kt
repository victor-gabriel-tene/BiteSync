package org.bytestorm.bitesync

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bytestorm.bitesync.localization.AppLanguage
import org.bytestorm.bitesync.localization.LocalStrings
import org.bytestorm.bitesync.localization.stringsFor
import org.bytestorm.bitesync.location.LocationTracker
import org.bytestorm.bitesync.network.ServerDiscovery
import org.bytestorm.bitesync.settings.SettingsRepository
import org.bytestorm.bitesync.settings.ThemeMode
import org.bytestorm.bitesync.ui.screen.LobbyScreen
import org.bytestorm.bitesync.ui.screen.MatchScreen
import org.bytestorm.bitesync.ui.screen.SettingsScreen
import org.bytestorm.bitesync.ui.screen.SuddenDeathScreen
import org.bytestorm.bitesync.ui.screen.SuggestScreen
import org.bytestorm.bitesync.ui.screen.SwipeScreen
import org.bytestorm.bitesync.ui.theme.BiteSyncTheme
import org.bytestorm.bitesync.viewmodel.AppScreen
import org.bytestorm.bitesync.viewmodel.BiteSyncViewModel

@Composable
fun App(
    serverDiscovery: ServerDiscovery,
    locationTracker: LocationTracker? = null,
    settingsRepository: SettingsRepository
) {
    var themeMode by remember {
        val saved = settingsRepository.getString("theme_mode", "system")
        mutableStateOf(ThemeMode.fromString(saved))
    }

    var currentLanguage by remember {
        val saved = settingsRepository.getString("app_language", "")
        if (saved.isEmpty()) {
            val deviceLang = settingsRepository.getDeviceLanguageCode()
            val detected = AppLanguage.fromCode(deviceLang)
            settingsRepository.putString("app_language", detected.code)
            mutableStateOf(detected)
        } else {
            mutableStateOf(AppLanguage.fromCode(saved))
        }
    }

    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalStrings provides stringsFor(currentLanguage)) {
        BiteSyncTheme(darkTheme = isDarkTheme) {
            val viewModel = viewModel { BiteSyncViewModel(serverDiscovery, locationTracker) }

            val screen by viewModel.screen.collectAsState()
            val roomState by viewModel.roomState.collectAsState()
            val venues by viewModel.venues.collectAsState()
            val currentIndex by viewModel.currentCardIndex.collectAsState()
            val predictions by viewModel.predictions.collectAsState()
            val isSearching by viewModel.isSearching.collectAsState()
            val error by viewModel.error.collectAsState()
            val isConnecting by viewModel.isConnecting.collectAsState()
            val myUserId by viewModel.myUserId.collectAsState()

            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = targetState.order() >= initialState.order()
                    if (forward) {
                        (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { it / 3 } + fadeOut(tween(250)))
                    }
                }
            ) { currentScreen ->
                when (currentScreen) {
                    is AppScreen.Lobby -> {
                        LobbyScreen(
                            roomState = roomState,
                            isConnecting = isConnecting,
                            error = error,
                            onCreateRoom = { name -> viewModel.createRoom(name) },
                            onJoinRoom = { pin, name -> viewModel.joinRoom(pin, name) },
                            onStartSuggesting = { viewModel.startSuggesting() },
                            onClearError = { viewModel.clearError() },
                            onOpenSettings = { viewModel.openSettings() }
                        )
                    }

                    is AppScreen.Settings -> {
                        SettingsScreen(
                            currentThemeMode = themeMode,
                            currentLanguage = currentLanguage,
                            onThemeModeChange = { mode ->
                                themeMode = mode
                                settingsRepository.putString("theme_mode", mode.name.lowercase())
                            },
                            onLanguageChange = { lang ->
                                currentLanguage = lang
                                settingsRepository.putString("app_language", lang.code)
                            },
                            onBack = { viewModel.closeSettings() }
                        )
                    }

                    is AppScreen.Suggesting -> {
                        SuggestScreen(
                            roomState = roomState,
                            predictions = predictions,
                            isSearching = isSearching,
                            error = error,
                            myUserId = myUserId,
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onPredictionSelected = { viewModel.onPredictionSelected(it) },
                            onStartSwiping = { viewModel.startSwiping() },
                            onToggleReady = { viewModel.toggleReady() },
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

                    is AppScreen.SuddenDeath -> {
                        SuddenDeathScreen(
                            venues = venues,
                            currentIndex = currentIndex,
                            round = currentScreen.round,
                            roomState = roomState,
                            onSwipe = { venueId, liked -> viewModel.onSwipe(venueId, liked) }
                        )
                    }

                    is AppScreen.Match -> {
                        MatchScreen(
                            venue = currentScreen.venue,
                            random = currentScreen.random,
                            onBackToLobby = { viewModel.returnToLobby() }
                        )
                    }
                }
            }
        }
    }
}
