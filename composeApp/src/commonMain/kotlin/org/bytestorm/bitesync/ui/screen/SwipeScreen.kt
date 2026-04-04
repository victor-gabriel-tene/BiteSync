package org.bytestorm.bitesync.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.ui.theme.BiteSyncTheme
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
    val gradient = BiteSyncTheme.gradients.main

    var swipeProgress by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        // Red glow on left edge (nope)
        if (swipeProgress < -0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF44336).copy(alpha = (-swipeProgress).coerceIn(0f, 0.5f)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        // Green glow on right edge (like)
        if (swipeProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF4CAF50).copy(alpha = swipeProgress.coerceIn(0f, 0.5f))
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Swiping", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (roomState != null) {
                        Text(
                            "Room: ${roomState.pin} \u00B7 ${roomState.users.size} users",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${currentIndex.coerceAtMost(venues.size)}/${venues.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress bar
            if (venues.isNotEmpty()) {
                val progress = (currentIndex.toFloat() / venues.size).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                    )
                }
            }

            SwipeableCardStack(
                venues = venues,
                currentIndex = currentIndex,
                onSwipe = { id, liked ->
                    swipeProgress = 0f
                    onSwipe(id, liked)
                },
                onSwipeProgress = { swipeProgress = it },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
