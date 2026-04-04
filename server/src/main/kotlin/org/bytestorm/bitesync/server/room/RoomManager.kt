package org.bytestorm.bitesync.server.room

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json
import org.bytestorm.bitesync.model.RoomStatus
import org.bytestorm.bitesync.model.ServerMessage
import org.bytestorm.bitesync.model.User
import org.bytestorm.bitesync.model.Venue
import java.util.concurrent.ConcurrentHashMap

class RoomManager(private val json: Json) {
    private val rooms = ConcurrentHashMap<String, Room>()

    fun createRoom(user: User, session: WebSocketSession): Room {
        val pin = generatePin()
        val room = Room(pin)
        room.users[user.id] = UserSession(user, session)
        rooms[pin] = room
        return room
    }

    fun joinRoom(pin: String, user: User, session: WebSocketSession): Room? {
        val room = rooms[pin] ?: return null
        room.users[user.id] = UserSession(user, session)
        return room
    }

    fun getRoom(pin: String): Room? = rooms[pin]

    fun updateStatus(pin: String, status: RoomStatus) {
        rooms[pin]?.status = status
    }

    fun submitVenue(pin: String, venue: Venue) {
        rooms[pin]?.addVenue(venue)
    }

    fun processSwipe(pin: String, userId: String, venueId: String, liked: Boolean): Venue? {
        val room = rooms[pin] ?: return null
        if (!liked) return null

        val likers = room.votes.getOrPut(venueId) { mutableSetOf() }
        likers.add(userId)

        if (likers.size >= room.users.size && room.users.isNotEmpty()) {
            return room.submittedVenues.find { it.id == venueId }
        }
        return null
    }

    fun removeUser(pin: String, userId: String): Room? {
        val room = rooms[pin] ?: return null
        room.users.remove(userId)
        if (room.users.isEmpty()) {
            rooms.remove(pin)
            return null
        }
        return room
    }

    suspend fun broadcastToRoom(pin: String, message: ServerMessage) {
        val room = rooms[pin] ?: return
        val text = json.encodeToString(ServerMessage.serializer(), message)
        room.users.values.forEach { userSession ->
            try {
                userSession.session.send(Frame.Text(text))
            } catch (_: Exception) { }
        }
    }

    private fun generatePin(): String {
        var pin: String
        do {
            pin = (1000..9999).random().toString()
        } while (rooms.containsKey(pin))
        return pin
    }
}
