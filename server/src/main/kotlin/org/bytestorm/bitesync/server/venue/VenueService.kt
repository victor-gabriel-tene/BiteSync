package org.bytestorm.bitesync.server.venue

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*
import org.bytestorm.bitesync.model.PlacePrediction
import org.bytestorm.bitesync.model.Venue

class VenueService {
    private val googleApiKey = loadApiKey().also {
        if (it.isEmpty()) println("[VenueService] WARNING: No Google API key found. Using mock data.")
        else println("[VenueService] Google API key loaded (${it.take(8)}...)")
    }

    private fun loadApiKey(): String {
        val fromEnv = System.getenv("GOOGLE_PLACES_API_KEY")
        if (!fromEnv.isNullOrBlank()) return fromEnv.trim().removeSurrounding("\"")

        val cwd = System.getProperty("user.dir") ?: "."
        println("[VenueService] Looking for .env in working dir: $cwd")

        val searchPaths = listOf(
            java.io.File(cwd, ".env"),
            java.io.File(cwd, "../.env"),
            java.io.File(cwd, "../../.env")
        )

        for (envFile in searchPaths) {
            if (envFile.exists()) {
                println("[VenueService] Found .env at: ${envFile.absolutePath}")
                val line = envFile.readLines().firstOrNull { it.startsWith("GOOGLE_PLACES_API_KEY=") }
                if (line != null) {
                    return line.substringAfter("=").trim().removeSurrounding("\"")
                }
            }
        }

        println("[VenueService] No .env file found in any of: ${searchPaths.map { it.absolutePath }}")
        return ""
    }

    fun isGoogleApiEnabled(): Boolean = googleApiKey.isNotEmpty()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // ---------- Autocomplete ----------

    suspend fun autocomplete(query: String, lat: Double?, lng: Double?, language: String? = null): List<PlacePrediction> {
        if (query.isBlank()) return emptyList()
        println("[VenueService] Autocomplete request: query='$query', lat=$lat, lng=$lng, lang=$language, usingGoogle=${googleApiKey.isNotEmpty()}")
        return if (googleApiKey.isNotEmpty()) {
            googleAutocomplete(query, lat, lng, language)
        } else {
            mockAutocomplete(query)
        }
    }

    private suspend fun googleAutocomplete(
        query: String, lat: Double?, lng: Double?, language: String? = null
    ): List<PlacePrediction> {
        val requestBody = buildJsonObject {
            put("input", query)
            put("includedPrimaryTypes", buildJsonArray { add("restaurant") })
            if (!language.isNullOrBlank()) {
                put("languageCode", language)
            }
            if (lat != null && lng != null) {
                put("locationBias", buildJsonObject {
                    put("circle", buildJsonObject {
                        put("center", buildJsonObject {
                            put("latitude", lat)
                            put("longitude", lng)
                        })
                        put("radius", 10000.0)
                    })
                })
            }
        }

        val responseText = client.post("https://places.googleapis.com/v1/places:autocomplete") {
            header("X-Goog-Api-Key", googleApiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }.bodyAsText()

        val response = Json.parseToJsonElement(responseText).jsonObject
        println("[VenueService] Google API response: ${responseText.take(200)}")

        val error = response["error"]?.jsonObject
        if (error != null) {
            println("[VenueService] API error: ${error["message"]?.jsonPrimitive?.contentOrNull}")
            return emptyList()
        }

        val suggestions = response["suggestions"]?.jsonArray ?: return emptyList()
        return suggestions.take(8).mapNotNull { element ->
            val prediction = element.jsonObject["placePrediction"]?.jsonObject ?: return@mapNotNull null
            val placeId = prediction["placeId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val structuredFormat = prediction["structuredFormat"]?.jsonObject
            val text = prediction["text"]?.jsonObject

            PlacePrediction(
                placeId = placeId,
                description = text?.get("text")?.jsonPrimitive?.contentOrNull ?: "",
                mainText = structuredFormat?.get("mainText")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "",
                secondaryText = structuredFormat?.get("secondaryText")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
            )
        }
    }

    // ---------- Place Details ----------

    suspend fun getPlaceDetails(placeId: String, language: String? = null): Venue? {
        return if (googleApiKey.isNotEmpty()) {
            googlePlaceDetails(placeId, language)
        } else {
            mockPlaceDetails(placeId)
        }
    }

    private suspend fun googlePlaceDetails(placeId: String, language: String? = null): Venue? {
        val fields = "id,displayName,rating,priceLevel,types,formattedAddress,photos"

        val responseText = client.get("https://places.googleapis.com/v1/places/$placeId") {
            header("X-Goog-Api-Key", googleApiKey)
            header("X-Goog-FieldMask", fields)
            if (!language.isNullOrBlank()) {
                parameter("languageCode", language)
            }
        }.bodyAsText()

        val result = Json.parseToJsonElement(responseText).jsonObject
        println("[VenueService] Place details response: ${responseText.take(300)}")

        val error = result["error"]?.jsonObject
        if (error != null) {
            println("[VenueService] Details API error: ${error["message"]?.jsonPrimitive?.contentOrNull}")
            return null
        }

        val photoName = result["photos"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull
        val photoUrl = photoName?.let {
            "https://places.googleapis.com/v1/$it/media?maxWidthPx=800&key=$googleApiKey"
        }

        val priceLevelStr = result["priceLevel"]?.jsonPrimitive?.contentOrNull
        val priceLevel = when (priceLevelStr) {
            "PRICE_LEVEL_FREE" -> 0
            "PRICE_LEVEL_INEXPENSIVE" -> 1
            "PRICE_LEVEL_MODERATE" -> 2
            "PRICE_LEVEL_EXPENSIVE" -> 3
            "PRICE_LEVEL_VERY_EXPENSIVE" -> 4
            else -> null
        }

        return Venue(
            id = result["id"]?.jsonPrimitive?.contentOrNull ?: placeId,
            name = result["displayName"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown",
            imageUrl = photoUrl,
            rating = result["rating"]?.jsonPrimitive?.doubleOrNull,
            priceLevel = priceLevel,
            categories = result["types"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.filter { it != "point_of_interest" && it != "establishment" }
                ?: emptyList(),
            address = result["formattedAddress"]?.jsonPrimitive?.contentOrNull ?: ""
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
