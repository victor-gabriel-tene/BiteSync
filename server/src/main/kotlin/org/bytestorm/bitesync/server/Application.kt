package org.bytestorm.bitesync.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import org.bytestorm.bitesync.model.ClientMessage
import org.bytestorm.bitesync.model.RoomStatus
import org.bytestorm.bitesync.model.ServerMessage
import org.bytestorm.bitesync.model.User
import org.bytestorm.bitesync.server.room.RoomManager
import org.bytestorm.bitesync.server.venue.VenueService
import java.time.Duration
import java.util.UUID

val appJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
    encodeDefaults = true
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureServer()
    }.start(wait = true)
}

fun Application.configureServer() {
    val roomManager = RoomManager(appJson)
    val venueService = VenueService()

    install(ContentNegotiation) {
        json(appJson)
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
    }

    install(WebSockets) {
        pingPeriodMillis = 15000L
        timeoutMillis = 15000L
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        // Google Places Autocomplete proxy
        get("/api/autocomplete") {
            val query = call.request.queryParameters["query"] ?: ""
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()

            val predictions = venueService.autocomplete(query, lat, lng)
            call.respond(predictions)
        }

        // Google Places Details proxy
        get("/api/place/{placeId}") {
            val placeId = call.parameters["placeId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "placeId is required"))
                return@get
            }

            val venue = venueService.getPlaceDetails(placeId)
            if (venue != null) {
                call.respond(venue)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Place not found"))
            }
        }

        // WebSocket: room management + swiping
        webSocket("/ws") {
            var userId: String? = null
            var roomPin: String? = null

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    val message = appJson.decodeFromString(ClientMessage.serializer(), text)

                    when (message) {
                        is ClientMessage.CreateRoom -> {
                            val user = User(
                                id = UUID.randomUUID().toString().take(8),
                                displayName = message.userName
                            )
                            userId = user.id
                            val room = roomManager.createRoom(user, this)
                            roomPin = room.pin

                            send(Frame.Text(
                                appJson.encodeToString(
                                    ServerMessage.serializer(),
                                    ServerMessage.RoomCreated(room.toRoomState())
                                )
                            ))
                        }

                        is ClientMessage.JoinRoom -> {
                            val user = User(
                                id = UUID.randomUUID().toString().take(8),
                                displayName = message.userName
                            )
                            userId = user.id
                            val room = roomManager.joinRoom(message.pin, user, this)

                            if (room != null) {
                                roomPin = room.pin
                                roomManager.broadcastToRoom(
                                    room.pin,
                                    ServerMessage.RoomUpdate(room.toRoomState())
                                )
                            } else {
                                send(Frame.Text(
                                    appJson.encodeToString(
                                        ServerMessage.serializer(),
                                        ServerMessage.Error("Room not found. Check the PIN and try again.")
                                    )
                                ))
                            }
                        }

                        is ClientMessage.StartSuggesting -> {
                            val pin = roomPin ?: continue
                            roomManager.updateStatus(pin, RoomStatus.SUGGESTING)
                            val room = roomManager.getRoom(pin)
                            if (room != null) {
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.RoomUpdate(room.toRoomState())
                                )
                            }
                        }

                        is ClientMessage.SubmitVenue -> {
                            val pin = roomPin ?: continue
                            val uid = userId ?: continue
                            roomManager.submitVenue(pin, message.venue)

                            val room = roomManager.getRoom(pin) ?: continue
                            val userName = room.users[uid]?.user?.displayName ?: "Someone"
                            roomManager.broadcastToRoom(
                                pin,
                                ServerMessage.VenueSubmitted(message.venue, userName)
                            )
                            roomManager.broadcastToRoom(
                                pin,
                                ServerMessage.RoomUpdate(room.toRoomState())
                            )
                        }

                        is ClientMessage.StartSwiping -> {
                            val pin = roomPin ?: continue
                            roomManager.updateStatus(pin, RoomStatus.SWIPING)
                            val room = roomManager.getRoom(pin) ?: continue

                            roomManager.broadcastToRoom(
                                pin,
                                ServerMessage.VenuesLoaded(room.submittedVenues.toList())
                            )
                            roomManager.broadcastToRoom(
                                pin,
                                ServerMessage.RoomUpdate(room.toRoomState())
                            )
                        }

                        is ClientMessage.Swipe -> {
                            val pin = roomPin ?: continue
                            val uid = userId ?: continue
                            val matchedVenue = roomManager.processSwipe(
                                pin, uid, message.venueId, message.liked
                            )
                            if (matchedVenue != null) {
                                roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.MatchFound(matchedVenue)
                                )
                            }
                        }
                    }
                }
            } finally {
                if (userId != null && roomPin != null) {
                    val room = roomManager.removeUser(roomPin!!, userId!!)
                    if (room != null) {
                        roomManager.broadcastToRoom(
                            roomPin!!,
                            ServerMessage.RoomUpdate(room.toRoomState())
                        )
                    }
                }
            }
        }
    }
}
