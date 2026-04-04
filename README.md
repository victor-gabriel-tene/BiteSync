# BiteSync

**Swipe together. Eat together.**

BiteSync is a real-time multiplayer app that helps groups of friends, coworkers, or family decide where to eat -- without the usual back-and-forth debate. Think of it as *Tinder for restaurants, but as a group activity*.

One person creates a room, everyone joins with a short PIN, the group suggests restaurants, and then everyone swipes left or right on each suggestion. When the group agrees on a place, it's a match!

---

## How It Works

### 1. Create or Join a Room
A host creates a room and receives a **4-digit PIN**. Everyone else joins by entering that PIN. A waiting screen shows who has connected, with avatar bubbles for each player.

### 2. Suggest Restaurants
All players search for and add restaurants to the shared pool. The search is powered by Google Places and is biased toward your current location, so nearby options show up first. Each suggestion displays its photo, star rating, price level, categories, and address. Once at least two restaurants have been added and everyone taps "Ready," the swiping begins.

### 3. Swipe
Each player sees a stack of cards -- one per restaurant -- and swipes **right to like** or **left to pass**. Cards show all the relevant details (photo, name, rating, price, category, address) and give visual feedback as you drag (green glow for like, red for nope). A progress bar tracks how many cards you've gone through.

### 4. Match or Sudden Death
- **Unanimous match** -- if every player likes the same restaurant, it wins instantly with a confetti celebration.
- **Tie** -- if the top-voted restaurants are tied, a **Sudden Death** round begins. Only the tied restaurants come back for another round of swiping. This can repeat multiple times until a winner emerges.
- **Random pick** -- if a tie truly can't be broken, one of the tied restaurants is picked at random.

### 5. Result
The winning restaurant is shown on a celebratory screen with all its details and an **"Open in Maps"** button that takes you straight to Google Maps for directions. Then hit **"Back to Lobby"** to start another round.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | **Kotlin** (2.3.20) |
| UI | **Compose Multiplatform** (1.10.3) with Material 3 |
| Platforms | **Android** (min SDK 24) and **iOS** (arm64 + simulator) |
| Architecture | **MVVM** -- ViewModel + StateFlow |
| Networking | **Ktor** (HTTP client + WebSocket client/server) |
| Real-time Communication | **WebSockets** for live room state sync |
| Serialization | **kotlinx.serialization** (JSON) |
| Image Loading | **Coil** 3.4.0 (multiplatform) |
| Server | **Ktor on Netty** (JVM) |
| External API | **Google Places API** (autocomplete + place details) |
| LAN Discovery | **UDP broadcast** on port 8888 for automatic server detection |

---

## Project Structure

```
BiteSync/
├── composeApp/          # Mobile client (Android + iOS)
│   └── src/
│       ├── commonMain/  # Shared UI, screens, ViewModel, components
│       ├── androidMain/ # Android-specific: location tracking, UDP discovery
│       └── iosMain/     # iOS-specific: CLLocation, POSIX socket discovery
│
├── shared/              # Code shared between client and server
│   └── src/commonMain/  # Data models (Venue, User, RoomState),
│                        # protocol messages (ClientMessage, ServerMessage),
│                        # and the network client (BiteSyncClient)
│
├── server/              # Backend server (JVM)
│   └── src/main/        # Ktor application: room management, voting logic,
│                        # Google Places API proxy, WebSocket handler
│
└── gradle/
    └── libs.versions.toml  # Centralized dependency versions
```

### Client Screens

| Screen | Purpose |
|---|---|
| **LobbyScreen** | Create or join a room using a PIN |
| **SuggestScreen** | Search for and submit restaurant suggestions |
| **SwipeScreen** | Tinder-style swipeable card stack |
| **SuddenDeathScreen** | Tiebreaker rounds for tied restaurants |
| **MatchScreen** | Winner display with confetti animation |

### Server Endpoints

| Endpoint | Type | Purpose |
|---|---|---|
| `/api/status` | REST | Health check |
| `/api/autocomplete` | REST | Proxied Google Places autocomplete |
| `/api/place/{placeId}` | REST | Proxied Google Places details |
| `/ws` | WebSocket | All room operations (join, suggest, vote, etc.) |

---

## Getting Started

### Prerequisites

- **JDK 17+**
- **Android Studio** or **IntelliJ IDEA** with the Kotlin Multiplatform plugin
- (Optional) A **Google Places API key** for real restaurant search. Without one, the server falls back to 25 mock restaurants.

### Running the Server

```shell
# Set your API key (optional -- mock data is used if missing)
set GOOGLE_PLACES_API_KEY=your_key_here

# Start the server
.\gradlew.bat :server:run
```

The server starts on the configured port and begins broadcasting its presence on the local network via UDP.

### Running the Android App

```shell
.\gradlew.bat :composeApp:assembleDebug
```

Or use the run configuration in Android Studio.

### Running the iOS App

Open the `iosApp/` directory in Xcode and run it from there, or use the iOS run configuration in your IDE.

### Connecting

If the server and clients are on the **same local network**, the app automatically discovers the server via UDP broadcast. No manual IP entry needed.

---

## Architecture Overview

BiteSync follows a **client-server** model with a **shared module** in between:

1. **`shared/`** defines all data models and the WebSocket protocol. Both the server and the client depend on this module, ensuring type-safe communication.

2. **`server/`** is a standalone Ktor application that manages rooms, coordinates the swiping/voting logic, proxies Google Places API calls, and broadcasts state updates to all connected clients over WebSockets.

3. **`composeApp/`** is the multiplatform mobile client. A single `BiteSyncViewModel` manages all application state, and `expect`/`actual` declarations handle platform differences (HTTP engine, GPS location, UDP socket discovery). Navigation is driven by a sealed `AppScreen` interface with animated transitions.

---

## License

This project is developed by **ByteStorm**.
