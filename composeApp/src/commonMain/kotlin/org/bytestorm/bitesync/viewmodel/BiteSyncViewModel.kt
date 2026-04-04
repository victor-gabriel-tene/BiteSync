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
import org.bytestorm.bitesync.location.LocationTracker
import org.bytestorm.bitesync.model.ClientMessage
import org.bytestorm.bitesync.model.PlacePrediction
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.model.RoomStatus
import org.bytestorm.bitesync.model.ServerMessage
import org.bytestorm.bitesync.model.Venue
import org.bytestorm.bitesync.network.BiteSyncClient
import org.bytestorm.bitesync.network.ServerDiscovery

sealed interface AppScreen {
    data object Lobby : AppScreen
    data object Suggesting : AppScreen
    data object Swiping : AppScreen
    data class SuddenDeath(val venues: List<Venue>, val round: Int) : AppScreen
    data class Match(val venue: Venue, val random: Boolean = false) : AppScreen
}

class BiteSyncViewModel(
    private val serverDiscovery: ServerDiscovery,
    private val locationTracker: LocationTracker? = null
) : ViewModel() {
    private var client: BiteSyncClient? = null

    init {
        serverDiscovery.startDiscovery()
        viewModelScope.launch {
            serverDiscovery.discoveredServerUrl.collect { url ->
                if (url != null && client == null) {
                    client = BiteSyncClient(url)
                }
            }
        }
    }

    // --- Navigation ---
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Lobby)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    // --- Room state ---
    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _myUserId = MutableStateFlow<String?>(null)
    val myUserId: StateFlow<String?> = _myUserId.asStateFlow()

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

    // --- Sudden Death ---
    private val _suddenDeathRound = MutableStateFlow(0)
    val suddenDeathRound: StateFlow<Int> = _suddenDeathRound.asStateFlow()

    private var _doneSent = false

    // --- General ---
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var searchJob: Job? = null

    // ======== Connection ========

    private fun connectAndExecute(action: suspend () -> Unit) {
        val currentClient = client
        if (currentClient == null) {
            _error.value = "Searching for server..."
            return
        }
        if (_isConnecting.value) return
        _isConnecting.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                viewModelScope.launch {
                    try {
                        currentClient.connectWebSocket { message -> handleServerMessage(message) }
                    } catch (e: Exception) {
                        _error.value = "WebSocket error: ${e.message}"
                    }
                }

                kotlinx.coroutines.withTimeout(5000L) {
                    currentClient.connected.first { it }
                }
                _isConnecting.value = false
                action()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _error.value = "Connection timeout. Is the server running?"
                _isConnecting.value = false
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
                _isConnecting.value = false
            }
        }
    }

    private fun sendMessage(message: ClientMessage) {
        val currentClient = client
        if (currentClient == null) {
            _error.value = "Searching for server..."
            return
        }
        if (currentClient.connected.value) {
            viewModelScope.launch { currentClient.send(message) }
        } else {
            connectAndExecute { currentClient.send(message) }
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
            val currentClient = client
            if (currentClient == null) {
                _error.value = "Searching for server..."
                return@launch
            }
            delay(300) // debounce
            _isSearching.value = true
            try {
                // Try to get user location first
                val location = locationTracker?.getCurrentLocation()
                _predictions.value = currentClient.autocomplete(
                    query = query,
                    lat = location?.first,
                    lng = location?.second
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun onPredictionSelected(prediction: PlacePrediction) {
        val currentClient = client
        if (currentClient == null) return
        _predictions.value = emptyList()
        viewModelScope.launch {
            try {
                val venue = currentClient.getPlaceDetails(prediction.placeId)
                currentClient.send(ClientMessage.SubmitVenue(venue))
            } catch (e: Exception) {
                _error.value = "Could not load place details: ${e.message}"
            }
        }
    }

    fun startSwiping() {
        sendMessage(ClientMessage.StartSwiping())
    }

    fun toggleReady() {
        val currentReady = _roomState.value?.users?.find { it.id == _myUserId.value }?.isReady ?: false
        sendMessage(ClientMessage.SetReady(!currentReady))
    }

    // ======== Swipe actions ========

    fun onSwipe(venueId: String, liked: Boolean) {
        val currentClient = client
        if (currentClient == null) return
        viewModelScope.launch {
            currentClient.send(ClientMessage.Swipe(venueId, liked))
        }
        _currentCardIndex.value++

        // Auto-send DoneSwiping when user has swiped all cards
        if (_currentCardIndex.value >= _venues.value.size && !_doneSent) {
            _doneSent = true
            viewModelScope.launch {
                currentClient.send(ClientMessage.DoneSwiping())
            }
        }
    }

    // ======== General ========

    fun clearError() {
        _error.value = null
    }

    fun returnToLobby() {
        _screen.value = AppScreen.Lobby
        _roomState.value = null
        _myUserId.value = null
        _venues.value = emptyList()
        _currentCardIndex.value = 0
        _predictions.value = emptyList()
        _error.value = null
        _isConnecting.value = false
        _suddenDeathRound.value = 0
        _doneSent = false
    }

    // ======== Server message handler ========

    private fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomCreated -> {
                _roomState.value = message.roomState
                _myUserId.value = message.userId
            }

            is ServerMessage.RoomJoined -> {
                _roomState.value = message.roomState
                _myUserId.value = message.userId
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
                    RoomStatus.SUDDEN_DEATH -> {} // Handled by ServerMessage.SuddenDeath
                    else -> {}
                }
            }

            is ServerMessage.VenueSubmitted -> {
                // Already handled via RoomUpdate which carries submittedVenues
            }

            is ServerMessage.VenuesLoaded -> {
                _venues.value = message.venues
                _currentCardIndex.value = 0
                _doneSent = false
                _screen.value = AppScreen.Swiping
            }

            is ServerMessage.SuddenDeath -> {
                _venues.value = message.venues
                _currentCardIndex.value = 0
                _doneSent = false
                _suddenDeathRound.value = message.round
                _screen.value = AppScreen.SuddenDeath(message.venues, message.round)
            }

            is ServerMessage.MatchFound -> {
                _screen.value = AppScreen.Match(message.venue, message.random)
            }

            is ServerMessage.Error -> {
                _error.value = message.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverDiscovery.stopDiscovery()
        client?.close()
    }

    companion object {
        const val SERVER_URL = "http://10.0.2.2:8080"
    }
}
