package org.bytestorm.bitesync.server.venue

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*
import org.bytestorm.bitesync.model.PlacePrediction
import org.bytestorm.bitesync.model.Venue

class VenueService {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }
    private val googleApiKey = System.getenv("GOOGLE_PLACES_API_KEY") ?: dotenv["GOOGLE_PLACES_API_KEY"] ?: ""

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // ---------- Autocomplete ----------

    suspend fun autocomplete(query: String, lat: Double?, lng: Double?): List<PlacePrediction> {
        if (query.isBlank()) return emptyList()
        return if (googleApiKey.isNotEmpty()) {
            googleAutocomplete(query, lat, lng)
        } else {
            mockAutocomplete(query)
        }
    }

    private suspend fun googleAutocomplete(
        query: String, lat: Double?, lng: Double?
    ): List<PlacePrediction> {
        val response: JsonObject = client.get(
            "https://maps.googleapis.com/maps/api/place/autocomplete/json"
        ) {
            parameter("input", query)
            parameter("types", "restaurant")
            parameter("key", googleApiKey)
            if (lat != null && lng != null) {
                parameter("location", "$lat,$lng")
                parameter("radius", 10000)
            }
        }.body()

        val predictions = response["predictions"]?.jsonArray ?: return emptyList()
        return predictions.take(8).map { element ->
            val pred = element.jsonObject
            val structured = pred["structured_formatting"]?.jsonObject
            PlacePrediction(
                placeId = pred["place_id"]?.jsonPrimitive?.contentOrNull ?: "",
                description = pred["description"]?.jsonPrimitive?.contentOrNull ?: "",
                mainText = structured?.get("main_text")?.jsonPrimitive?.contentOrNull ?: "",
                secondaryText = structured?.get("secondary_text")?.jsonPrimitive?.contentOrNull ?: ""
            )
        }
    }

    // ---------- Place Details ----------

    suspend fun getPlaceDetails(placeId: String): Venue? {
        return if (googleApiKey.isNotEmpty()) {
            googlePlaceDetails(placeId)
        } else {
            mockPlaceDetails(placeId)
        }
    }

    private suspend fun googlePlaceDetails(placeId: String): Venue? {
        val response: JsonObject = client.get(
            "https://maps.googleapis.com/maps/api/place/details/json"
        ) {
            parameter("place_id", placeId)
            parameter("fields", "place_id,name,rating,price_level,types,vicinity,photos,formatted_address")
            parameter("key", googleApiKey)
        }.body()

        val result = response["result"]?.jsonObject ?: return null
        val photoRef = result["photos"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("photo_reference")?.jsonPrimitive?.contentOrNull
        val photoUrl = photoRef?.let {
            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=$it&key=$googleApiKey"
        }

        return Venue(
            id = result["place_id"]?.jsonPrimitive?.contentOrNull ?: placeId,
            name = result["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
            imageUrl = photoUrl,
            rating = result["rating"]?.jsonPrimitive?.doubleOrNull,
            priceLevel = result["price_level"]?.jsonPrimitive?.intOrNull,
            categories = result["types"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.filter { it != "point_of_interest" && it != "establishment" }
                ?: emptyList(),
            address = result["formatted_address"]?.jsonPrimitive?.contentOrNull
                ?: result["vicinity"]?.jsonPrimitive?.contentOrNull
                ?: ""
        )
    }

    // ---------- Mock data (no API key) ----------

    private val mockRestaurants = listOf(
        MockPlace("mock_1", "Sakura Sushi Bar", "Japanese, Sushi", "120 E 39th St, New York", 4.6, 3, "https://picsum.photos/seed/sushi/800/600"),
        MockPlace("mock_2", "Golden Dragon Palace", "Chinese, Dim Sum", "88 Mott St, New York", 4.2, 2, "https://picsum.photos/seed/chinese/800/600"),
        MockPlace("mock_3", "Bella Italia Trattoria", "Italian, Pasta", "225 Mulberry St, New York", 4.5, 3, "https://picsum.photos/seed/italian/800/600"),
        MockPlace("mock_4", "Thai Basil Kitchen", "Thai, Curry", "661 Washington Ave, Brooklyn", 4.4, 2, "https://picsum.photos/seed/thai/800/600"),
        MockPlace("mock_5", "Taco Loco Cantina", "Mexican, Tacos", "45 7th Ave S, New York", 4.3, 1, "https://picsum.photos/seed/mexican/800/600"),
        MockPlace("mock_6", "Maharaja Indian Grill", "Indian, Tandoori", "150 E 50th St, New York", 4.1, 2, "https://picsum.photos/seed/indian/800/600"),
        MockPlace("mock_7", "Le Petit Bistro", "French, Bistro", "312 W 4th St, New York", 4.7, 4, "https://picsum.photos/seed/french/800/600"),
        MockPlace("mock_8", "Seoul BBQ House", "Korean, BBQ", "5 W 36th St, New York", 4.3, 2, "https://picsum.photos/seed/korean/800/600"),
        MockPlace("mock_9", "Olive & Vine Mediterranean", "Mediterranean, Falafel", "78 2nd Ave, New York", 4.4, 2, "https://picsum.photos/seed/med/800/600"),
        MockPlace("mock_10", "Big Apple Burger Joint", "American, Burgers", "200 W 44th St, New York", 4.0, 1, "https://picsum.photos/seed/burger/800/600"),
        MockPlace("mock_11", "Pho Saigon Express", "Vietnamese, Pho", "85 Baxter St, New York", 4.5, 1, "https://picsum.photos/seed/viet/800/600"),
        MockPlace("mock_12", "Olympia Greek Taverna", "Greek, Gyros", "310 W 51st St, New York", 4.2, 2, "https://picsum.photos/seed/greek/800/600"),
        MockPlace("mock_13", "Addis Ethiopian Kitchen", "Ethiopian, Injera", "140 St Marks Pl, New York", 4.6, 2, "https://picsum.photos/seed/ethiopian/800/600"),
        MockPlace("mock_14", "Istanbul Kebab House", "Turkish, Kebab", "99 MacDougal St, New York", 4.3, 2, "https://picsum.photos/seed/turkish/800/600"),
        MockPlace("mock_15", "Lima Peruvian Grill", "Peruvian, Ceviche", "60 Greenwich Ave, New York", 4.5, 3, "https://picsum.photos/seed/peruvian/800/600"),
        MockPlace("mock_16", "Ramen Ichiban", "Japanese, Ramen", "175 2nd Ave, New York", 4.4, 2, "https://picsum.photos/seed/ramen/800/600"),
        MockPlace("mock_17", "Pizza Napoli Authentic", "Italian, Pizza", "68 University Pl, New York", 4.6, 2, "https://picsum.photos/seed/pizza/800/600"),
        MockPlace("mock_18", "Spice Route Curry House", "Indian, Biryani", "123 Lexington Ave, New York", 4.2, 2, "https://picsum.photos/seed/curry/800/600"),
        MockPlace("mock_19", "Lucky Panda Noodles", "Chinese, Noodles", "52 Bayard St, New York", 4.1, 1, "https://picsum.photos/seed/noodles/800/600"),
        MockPlace("mock_20", "El Camino Burritos", "Mexican, Burritos", "170 Bedford Ave, Brooklyn", 4.3, 1, "https://picsum.photos/seed/burrito/800/600"),
        MockPlace("mock_21", "The Smokehouse BBQ", "American, BBQ", "105 S 6th St, Brooklyn", 4.4, 2, "https://picsum.photos/seed/bbq/800/600"),
        MockPlace("mock_22", "Crepe & Co Paris", "French, Crepes", "85 MacDougal St, New York", 4.5, 2, "https://picsum.photos/seed/crepe/800/600"),
        MockPlace("mock_23", "Green Bowl Vegan", "Vegan, Healthy", "55 Irving Pl, New York", 4.3, 2, "https://picsum.photos/seed/vegan/800/600"),
        MockPlace("mock_24", "Catch Seafood Bar", "Seafood, Raw Bar", "210 Spring St, New York", 4.6, 4, "https://picsum.photos/seed/seafood/800/600"),
        MockPlace("mock_25", "Wrap Star Shawarma", "Middle Eastern, Shawarma", "48 Macdougal St, New York", 4.2, 1, "https://picsum.photos/seed/shawarma/800/600"),
    )

    private fun mockAutocomplete(query: String): List<PlacePrediction> {
        val q = query.lowercase()
        return mockRestaurants
            .filter { it.name.lowercase().contains(q) || it.categories.lowercase().contains(q) }
            .take(8)
            .map { place ->
                PlacePrediction(
                    placeId = place.id,
                    description = "${place.name} - ${place.categories}",
                    mainText = place.name,
                    secondaryText = place.address
                )
            }
    }

    private fun mockPlaceDetails(placeId: String): Venue? {
        val place = mockRestaurants.find { it.id == placeId } ?: return null
        return Venue(
            id = place.id,
            name = place.name,
            imageUrl = place.imageUrl,
            rating = place.rating,
            priceLevel = place.priceLevel,
            categories = place.categories.split(", "),
            address = place.address
        )
    }

    private data class MockPlace(
        val id: String,
        val name: String,
        val categories: String,
        val address: String,
        val rating: Double,
        val priceLevel: Int,
        val imageUrl: String
    )
}
