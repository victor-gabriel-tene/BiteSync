package org.bytestorm.bitesync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bytestorm.bitesync.model.ClientMessage
import org.bytestorm.bitesync.model.PlacePrediction
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.model.RoomStatus
import org.bytestorm.bitesync.model.ServerMessage
import org.bytestorm.bitesync.model.Venue
import org.bytestorm.bitesync.network.BiteSyncClient

sealed interface AppScreen {
    data object Lobby : AppScreen
    data object Suggesting : AppScreen
    data object Swiping : AppScreen
    data class Match(val venue: Venue) : AppScreen
}

class BiteSyncViewModel : ViewModel() {
    private val client = BiteSyncClient(SERVER_URL)

    // --- Navigation ---
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Lobby)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    // --- Room state ---
    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    // --- Swiping ---
    private val _venues = MutableStateFlow<List<Venue>>(emptyList())
    val venues: StateFlow<List<Venue>> = _venues.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    // --- Autocomplete ---
    private val _predictions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val predictions: StateFlow<List<PlacePrediction>> = _predictions.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // --- General ---
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var searchJob: Job? = null

    // ======== Connection ========

    private fun connectAndExecute(action: suspend () -> Unit) {
        _isConnecting.value = true
        viewModelScope.launch {
            try {
                launch {
                    client.connectWebSocket { message -> handleServerMessage(message) }
                }
                client.connected.first { it }
                _isConnecting.value = false
                action()
            } catch (e: Exception) {
                _error.value = e.message ?: "Connection failed"
                _isConnecting.value = false
            }
        }
    }

    private fun sendMessage(message: ClientMessage) {
        if (client.connected.value) {
            viewModelScope.launch { client.send(message) }
        } else {
            connectAndExecute { client.send(message) }
        }
    }

    // ======== Lobby actions ========

    fun createRoom(userName: String) {
        sendMessage(ClientMessage.CreateRoom(userName))
    }

    fun joinRoom(pin: String, userName: String) {
        sendMessage(ClientMessage.JoinRoom(pin, userName))
    }

    fun startSuggesting() {
        sendMessage(ClientMessage.StartSuggesting())
    }

    // ======== Suggest actions ========

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _predictions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _isSearching.value = true
            try {
                _predictions.value = client.autocomplete(query)
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun onPredictionSelected(prediction: PlacePrediction) {
        _predictions.value = emptyList()
        viewModelScope.launch {
            try {
                val venue = client.getPlaceDetails(prediction.placeId)
                client.send(ClientMessage.SubmitVenue(venue))
            } catch (e: Exception) {
                _error.value = "Could not load place details: ${e.message}"
            }
        }
    }

    fun startSwiping() {
        sendMessage(ClientMessage.StartSwiping())
    }

    // ======== Swipe actions ========

    fun onSwipe(venueId: String, liked: Boolean) {
        viewModelScope.launch {
            client.send(ClientMessage.Swipe(venueId, liked))
        }
        _currentCardIndex.value++
    }

    // ======== General ========

    fun clearError() {
        _error.value = null
    }

    fun returnToLobby() {
        _screen.value = AppScreen.Lobby
        _roomState.value = null
        _venues.value = emptyList()
        _currentCardIndex.value = 0
        _predictions.value = emptyList()
    }

    // ======== Server message handler ========

    private fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomCreated -> {
                _roomState.value = message.roomState
            }

            is ServerMessage.RoomUpdate -> {
                _roomState.value = message.roomState
                when (message.roomState.status) {
                    RoomStatus.SUGGESTING -> {
                        if (_screen.value !is AppScreen.Suggesting) {
                            _screen.value = AppScreen.Suggesting
                        }
                    }
                    RoomStatus.SWIPING -> {
                        if (_screen.value !is AppScreen.Swiping) {
                            _screen.value = AppScreen.Swiping
                        }
                    }
                    else -> {}
                }
            }

            is ServerMessage.VenueSubmitted -> {
                // Already handled via RoomUpdate which carries submittedVenues
            }

            is ServerMessage.VenuesLoaded -> {
                _venues.value = message.venues
                _currentCardIndex.value = 0
                _screen.value = AppScreen.Swiping
            }

            is ServerMessage.MatchFound -> {
                _screen.value = AppScreen.Match(message.venue)
            }

            is ServerMessage.Error -> {
                _error.value = message.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    companion object {
        const val SERVER_URL = "http://10.0.2.2:8080"
    }
}
