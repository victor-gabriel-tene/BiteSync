package org.bytestorm.bitesync.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.model.Venue
import org.bytestorm.bitesync.ui.components.SwipeableCardStack

@Composable
fun SwipeScreen(
    venues: List<Venue>,
    currentIndex: Int,
    roomState: RoomState?,
    onSwipe: (venueId: String, liked: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
    )

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Swiping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (roomState != null) {
                        Text(
                            "Room: ${roomState.pin} \u00B7 ${roomState.users.size} players",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${currentIndex.coerceAtMost(venues.size)}/${venues.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SwipeableCardStack(
                venues = venues,
                currentIndex = currentIndex,
                onSwipe = onSwipe,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2190", fontSize = 28.sp, color = Color.White)
                    Text("Nope", color = Color(0xFFF44336), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2192", fontSize = 28.sp, color = Color.White)
                    Text("Like", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
