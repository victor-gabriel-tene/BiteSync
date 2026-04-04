package org.bytestorm.bitesync.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.model.Venue
import org.bytestorm.bitesync.ui.components.SwipeableCardStack

@Composable
fun SuddenDeathScreen(
    venues: List<Venue>,
    currentIndex: Int,
    round: Int,
    roomState: RoomState?,
    onSwipe: (venueId: String, liked: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSplash by remember(round) { mutableStateOf(true) }

    LaunchedEffect(round) {
        showSplash = true
        delay(2500)
        showSplash = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Swipe phase behind the splash
        SuddenDeathSwipeContent(
            venues = venues,
            currentIndex = currentIndex,
            round = round,
            roomState = roomState,
            onSwipe = onSwipe
        )

        // Dramatic splash overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400))
        ) {
            SuddenDeathSplash(round = round, venueCount = venues.size)
        }
    }
}

@Composable
private fun SuddenDeathSplash(round: Int, venueCount: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1a0000), Color(0xFF8B0000), Color(0xFF1a0000))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "\u2694\uFE0F",
                fontSize = 64.sp,
                modifier = Modifier.scale(pulse)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "SUDDEN DEATH",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF4444),
                modifier = Modifier.scale(pulse),
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Round $round",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            var textVisible by remember(round) { mutableStateOf(false) }
            LaunchedEffect(round) {
                delay(800)
                textVisible = true
            }

            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(tween(600)) + scaleIn(tween(600))
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        "$venueCount restaurants remain...",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuddenDeathSwipeContent(
    venues: List<Venue>,
    currentIndex: Int,
    round: Int,
    roomState: RoomState?,
    onSwipe: (venueId: String, liked: Boolean) -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2a0a0a), Color(0xFF1a1a2e))
    )

    Box(modifier = Modifier.fillMaxSize().background(gradient)) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "\u2694\uFE0F",
                            fontSize = 18.sp
                        )
                        Text(
                            "SUDDEN DEATH",
                            color = Color(0xFFFF4444),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp
                        )
                    }
                    if (roomState != null) {
                        Text(
                            "Round $round \u00B7 ${roomState.users.size} players",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF4444).copy(alpha = 0.2f)
                ) {
                    Text(
                        "${currentIndex.coerceAtMost(venues.size)}/${venues.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color(0xFFFF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress bar (red-themed)
            if (venues.isNotEmpty()) {
                val progress = (currentIndex.toFloat() / venues.size).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF4444), Color(0xFFFF8800))
                                )
                            )
                    )
                }
            }

            SwipeableCardStack(
                venues = venues,
                currentIndex = currentIndex,
                onSwipe = onSwipe,
                modifier = Modifier.weight(1f)
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u2715", fontSize = 22.sp, color = Color(0xFFF44336))
                    }
                    Text(
                        "Nope", color = Color(0xFFF44336), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u2665", fontSize = 22.sp, color = Color(0xFF4CAF50))
                    }
                    Text(
                        "Like", color = Color(0xFF4CAF50), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
