package org.bytestorm.bitesync.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientMessage {
    @Serializable
    @SerialName("create_room")
    data class CreateRoom(val userName: String) : ClientMessage

    @Serializable
    @SerialName("join_room")
    data class JoinRoom(val pin: String, val userName: String) : ClientMessage

    /** Host transitions room from WAITING → SUGGESTING */
    @Serializable
    @SerialName("start_suggesting")
    data class StartSuggesting(val lat: Double = 0.0, val lng: Double = 0.0) : ClientMessage

    /** User submits a venue they picked from autocomplete */
    @Serializable
    @SerialName("submit_venue")
    data class SubmitVenue(val venue: Venue) : ClientMessage

    /** Host transitions room from SUGGESTING → SWIPING */
    @Serializable
    @SerialName("start_swiping")
    data class StartSwiping(val unused: Int = 0) : ClientMessage

    @Serializable
    @SerialName("swipe")
    data class Swipe(val venueId: String, val liked: Boolean) : ClientMessage
}

@Serializable
sealed interface ServerMessage {
    @Serializable
    @SerialName("room_created")
    data class RoomCreated(val roomState: RoomState) : ServerMessage

    @Serializable
    @SerialName("room_update")
    data class RoomUpdate(val roomState: RoomState) : ServerMessage

    /** Broadcast when someone adds a venue during the suggestion phase */
    @Serializable
    @SerialName("venue_submitted")
    data class VenueSubmitted(val venue: Venue, val byUser: String) : ServerMessage

    @Serializable
    @SerialName("venues_loaded")
    data class VenuesLoaded(val venues: List<Venue>) : ServerMessage

    @Serializable
    @SerialName("match_found")
    data class MatchFound(val venue: Venue) : ServerMessage

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : ServerMessage
}
