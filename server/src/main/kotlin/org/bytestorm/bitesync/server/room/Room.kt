package org.bytestorm.bitesync.server.room

import io.ktor.websocket.WebSocketSession
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.model.RoomStatus
import org.bytestorm.bitesync.model.User
import org.bytestorm.bitesync.model.Venue

data class UserSession(
    val user: User,
    val session: WebSocketSession
)

class Room(val pin: String) {
    val users = mutableMapOf<String, UserSession>()
    var status: RoomStatus = RoomStatus.WAITING

    val submittedVenues = mutableListOf<Venue>()
    val votes = mutableMapOf<String, MutableSet<String>>()

    fun addVenue(venue: Venue) {
        if (submittedVenues.none { it.id == venue.id }) {
            submittedVenues.add(venue)
        }
    }

    fun toRoomState(): RoomState = RoomState(
        pin = pin,
        users = users.values.map { it.user },
        status = status,
        submittedVenues = submittedVenues.toList()
    )
}
