package org.bytestorm.bitesync.model

import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val rating: Double? = null,
    val priceLevel: Int? = null,
    val categories: List<String> = emptyList(),
    val address: String = "",
    val distance: Double? = null
)

@Serializable
data class User(
    val id: String,
    val displayName: String
)

@Serializable
data class PlacePrediction(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String = ""
)

@Serializable
enum class RoomStatus {
    WAITING, SUGGESTING, SWIPING, MATCH_FOUND
}

@Serializable
data class RoomState(
    val pin: String,
    val users: List<User> = emptyList(),
    val status: RoomStatus = RoomStatus.WAITING,
    val submittedVenues: List<Venue> = emptyList()
)
