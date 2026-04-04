package org.bytestorm.bitesync.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.bytestorm.bitesync.model.ClientMessage
import org.bytestorm.bitesync.model.PlacePrediction
import org.bytestorm.bitesync.model.ServerMessage
import org.bytestorm.bitesync.model.Venue

class BiteSyncClient(private val baseUrl: String) {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private val httpClient = HttpClient(httpClientEngine()) {
        install(ContentNegotiation) {
            json(this@BiteSyncClient.json)
        }
        install(WebSockets)
    }

    private var wsSession: WebSocketSession? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    // --- REST: Autocomplete ---

    suspend fun autocomplete(query: String, lat: Double? = null, lng: Double? = null, language: String? = null): List<PlacePrediction> {
        return httpClient.get("$baseUrl/api/autocomplete") {
            parameter("query", query)
            lat?.let { parameter("lat", it) }
            lng?.let { parameter("lng", it) }
            language?.let { parameter("language", it) }
        }.body()
    }

    // --- REST: Place Details ---

    suspend fun getPlaceDetails(placeId: String, language: String? = null): Venue {
        return httpClient.get("$baseUrl/api/place/$placeId") {
            language?.let { parameter("language", it) }
        }.body()
    }

    // --- WebSocket ---

    suspend fun connectWebSocket(onMessage: suspend (ServerMessage) -> Unit) {
        val wsUrl = baseUrl.replace("http", "ws")
        httpClient.webSocket("$wsUrl/ws") {
            wsSession = this
            _connected.value = true
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val message = json.decodeFromString(ServerMessage.serializer(), text)
                        onMessage(message)
                    }
                }
            } finally {
                wsSession = null
                _connected.value = false
            }
        }
    }

    suspend fun send(message: ClientMessage) {
        val text = json.encodeToString(ClientMessage.serializer(), message)
        wsSession?.send(Frame.Text(text))
    }

    fun close() {
        httpClient.close()
    }
}
