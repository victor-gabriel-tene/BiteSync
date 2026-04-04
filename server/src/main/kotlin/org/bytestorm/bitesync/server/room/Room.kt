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

    // Tracks which users have finished swiping all cards
    val finishedUsers = mutableSetOf<String>()

    // Attendance tracking (after match is found)
    val attendanceVotes = mutableMapOf<String, Boolean>()
    var matchedVenue: Venue? = null

    // Sudden death state
    var suddenDeathRound: Int = 0
    val suddenDeathVenues = mutableListOf<Venue>()
    val suddenDeathVotes = mutableMapOf<String, MutableSet<String>>()
    val suddenDeathFinishedUsers = mutableSetOf<String>()
    private var previousSuddenDeathTopCount: Int = -1

    fun addVenue(venue: Venue) {
        if (submittedVenues.none { it.id == venue.id }) {
            submittedVenues.add(venue)
        }
    }

    fun resetForSuddenDeathRound(venues: List<Venue>) {
        suddenDeathRound++
        suddenDeathVenues.clear()
        suddenDeathVenues.addAll(venues)
        suddenDeathVotes.clear()
        suddenDeathFinishedUsers.clear()
        previousSuddenDeathTopCount = venues.size
    }

    fun getPreviousSuddenDeathTopCount(): Int = previousSuddenDeathTopCount

    fun toRoomState(): RoomState = RoomState(
        pin = pin,
        users = users.values.map { it.user },
        status = status,
        submittedVenues = submittedVenues.toList()
    )
}
