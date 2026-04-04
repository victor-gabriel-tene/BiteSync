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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.Duration
import java.util.UUID
import kotlin.concurrent.thread

val appJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
    encodeDefaults = true
}

fun main() {
    val port = 8080
    
    // UDP Broadcaster for auto-discovery
    thread(isDaemon = true) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val message = "BITESYNC_SERVER:$port".toByteArray()
            val address = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(message, message.size, address, 8888)
            
            println("Starting discovery broadcaster...")
            while (true) {
                socket.send(packet)
                Thread.sleep(2000)
            }
        } catch (e: Exception) {
            println("Discovery broadcaster failed: ${e.message}")
        }
    }

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
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

        // Health check / API key status
        get("/api/status") {
            call.respond(mapOf(
                "server" to "running",
                "googleApiKey" to if (venueService.isGoogleApiEnabled()) "configured" else "MISSING - using mock data"
            ))
        }

        // Google Places Autocomplete proxy
        get("/api/autocomplete") {
            val query = call.request.queryParameters["query"] ?: ""
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val language = call.request.queryParameters["language"]

            val predictions = venueService.autocomplete(query, lat, lng, language)
            call.respond(predictions)
        }

        // Google Places Details proxy
        get("/api/place/{placeId}") {
            val placeId = call.parameters["placeId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "placeId is required"))
                return@get
            }
            val language = call.request.queryParameters["language"]

            val venue = venueService.getPlaceDetails(placeId, language)
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
                                    ServerMessage.RoomCreated(room.toRoomState(), user.id)
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
                                send(Frame.Text(
                                    appJson.encodeToString(
                                        ServerMessage.serializer(),
                                        ServerMessage.RoomJoined(room.toRoomState(), user.id)
                                    )
                                ))
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

                        is ClientMessage.SetReady -> {
                            val pin = roomPin ?: continue
                            val uid = userId ?: continue
                            val allReady = roomManager.setUserReady(pin, uid, message.ready)
                            val room = roomManager.getRoom(pin) ?: continue

                            roomManager.broadcastToRoom(
                                pin,
                                ServerMessage.RoomUpdate(room.toRoomState())
                            )

                            if (allReady && room.status == RoomStatus.SUGGESTING && room.submittedVenues.isNotEmpty()) {
                                roomManager.updateStatus(pin, RoomStatus.SWIPING)
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.VenuesLoaded(room.submittedVenues.toList())
                                )
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.RoomUpdate(room.toRoomState())
                                )
                            }
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
                            val room = roomManager.getRoom(pin) ?: continue

                            if (room.status == RoomStatus.SUDDEN_DEATH) {
                                roomManager.processSuddenDeathSwipe(pin, uid, message.venueId, message.liked)
                            } else {
                                val matchedVenue = roomManager.processSwipe(
                                    pin, uid, message.venueId, message.liked
                                )
                                if (matchedVenue != null) {
                                    room.matchedVenue = matchedVenue
                                    roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                    roomManager.broadcastToRoom(
                                        pin,
                                        ServerMessage.MatchFound(matchedVenue)
                                    )
                                }
                            }
                        }

                        is ClientMessage.SetAttendance -> {
                            val pin = roomPin ?: continue
                            val uid = userId ?: continue
                            val allResponded = roomManager.setAttendance(pin, uid, message.attending)
                            val room = roomManager.getRoom(pin) ?: continue

                            if (allResponded) {
                                val venue = room.matchedVenue ?: continue
                                val attendees = roomManager.getAttendees(pin)
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.FinalPlan(venue, attendees)
                                )
                            } else {
                                roomManager.broadcastToRoom(
                                    pin,
                                    ServerMessage.AttendanceUpdate(
                                        attendance = room.attendanceVotes.toMap(),
                                        allResponded = false
                                    )
                                )
                            }
                        }

                        is ClientMessage.DoneSwiping -> {
                            val pin = roomPin ?: continue
                            val uid = userId ?: continue
                            val room = roomManager.getRoom(pin) ?: continue

                            if (room.status == RoomStatus.SUDDEN_DEATH) {
                                val allDone = roomManager.markSuddenDeathUserDone(pin, uid)
                                if (allDone) {
                                    when (val result = roomManager.resolveSuddenDeath(pin)) {
                                        is RoomManager.SwipeResult.Winner -> {
                                            room.matchedVenue = result.venue
                                            roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                            roomManager.broadcastToRoom(pin, ServerMessage.MatchFound(result.venue, random = result.random))
                                        }
                                        is RoomManager.SwipeResult.Tied -> {
                                            room.resetForSuddenDeathRound(result.venues)
                                            roomManager.broadcastToRoom(
                                                pin,
                                                ServerMessage.SuddenDeath(result.venues, room.suddenDeathRound)
                                            )
                                        }
                                        is RoomManager.SwipeResult.NoLikes -> {
                                            val randomWinner = room.suddenDeathVenues.random()
                                            room.matchedVenue = randomWinner
                                            roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                            roomManager.broadcastToRoom(pin, ServerMessage.MatchFound(randomWinner, random = true))
                                        }
                                    }
                                }
                            } else {
                                val allDone = roomManager.markUserDone(pin, uid)
                                if (allDone) {
                                    when (val result = roomManager.calculateTopVenues(pin)) {
                                        is RoomManager.SwipeResult.Winner -> {
                                            room.matchedVenue = result.venue
                                            roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                            roomManager.broadcastToRoom(pin, ServerMessage.MatchFound(result.venue, random = result.random))
                                        }
                                        is RoomManager.SwipeResult.Tied -> {
                                            roomManager.updateStatus(pin, RoomStatus.SUDDEN_DEATH)
                                            room.resetForSuddenDeathRound(result.venues)
                                            roomManager.broadcastToRoom(
                                                pin,
                                                ServerMessage.SuddenDeath(result.venues, room.suddenDeathRound)
                                            )
                                        }
                                        is RoomManager.SwipeResult.NoLikes -> {
                                            val randomWinner = room.submittedVenues.random()
                                            room.matchedVenue = randomWinner
                                            roomManager.updateStatus(pin, RoomStatus.MATCH_FOUND)
                                            roomManager.broadcastToRoom(pin, ServerMessage.MatchFound(randomWinner, random = true))
                                        }
                                    }
                                }
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
