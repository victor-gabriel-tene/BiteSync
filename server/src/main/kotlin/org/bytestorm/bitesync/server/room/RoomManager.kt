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

    fun setUserReady(pin: String, userId: String, ready: Boolean): Boolean {
        val room = rooms[pin] ?: return false
        val userSession = room.users[userId] ?: return false
        room.users[userId] = userSession.copy(user = userSession.user.copy(isReady = ready))

        return room.users.values.all { it.user.isReady }
    }

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

    // ======== Done Swiping / Sudden Death ========

    /**
     * Mark a user as finished swiping. Returns true when ALL users are done.
     */
    fun markUserDone(pin: String, userId: String): Boolean {
        val room = rooms[pin] ?: return false
        room.finishedUsers.add(userId)
        return room.finishedUsers.size >= room.users.size
    }

    /**
     * After all users finish swiping, find the venues with the most likes.
     * Returns null if no venues were liked at all (edge case).
     */
    sealed class SwipeResult {
        data class Winner(val venue: Venue, val random: Boolean = false) : SwipeResult()
        data class Tied(val venues: List<Venue>) : SwipeResult()
        data object NoLikes : SwipeResult()
    }

    fun calculateTopVenues(pin: String): SwipeResult {
        val room = rooms[pin] ?: return SwipeResult.NoLikes
        if (room.votes.isEmpty()) return SwipeResult.NoLikes

        val maxLikes = room.votes.values.maxOf { it.size }
        val topVenueIds = room.votes.filter { it.value.size == maxLikes }.keys
        val topVenues = room.submittedVenues.filter { it.id in topVenueIds }

        return if (topVenues.size == 1) {
            SwipeResult.Winner(topVenues.first())
        } else {
            SwipeResult.Tied(topVenues)
        }
    }

    /**
     * Process a swipe during a sudden death round.
     */
    fun processSuddenDeathSwipe(pin: String, userId: String, venueId: String, liked: Boolean) {
        val room = rooms[pin] ?: return
        if (!liked) return
        val likers = room.suddenDeathVotes.getOrPut(venueId) { mutableSetOf() }
        likers.add(userId)
    }

    /**
     * Mark a user as finished in the current sudden death round.
     * Returns true when ALL users are done with this round.
     */
    fun markSuddenDeathUserDone(pin: String, userId: String): Boolean {
        val room = rooms[pin] ?: return false
        room.suddenDeathFinishedUsers.add(userId)
        return room.suddenDeathFinishedUsers.size >= room.users.size
    }

    /**
     * After all users finish a sudden death round, resolve the outcome.
     */
    fun resolveSuddenDeath(pin: String): SwipeResult {
        val room = rooms[pin] ?: return SwipeResult.NoLikes
        val currentVenues = room.suddenDeathVenues

        if (room.suddenDeathVotes.isEmpty()) {
            return SwipeResult.Winner(currentVenues.random(), random = true)
        }

        val maxLikes = room.suddenDeathVotes.values.maxOf { it.size }
        val topVenueIds = room.suddenDeathVotes.filter { it.value.size == maxLikes }.keys
        val topVenues = currentVenues.filter { it.id in topVenueIds }

        return when {
            topVenues.size == 1 -> SwipeResult.Winner(topVenues.first())
            topVenues.size >= room.getPreviousSuddenDeathTopCount() -> {
                SwipeResult.Winner(topVenues.random(), random = true)
            }
            else -> SwipeResult.Tied(topVenues)
        }
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
