package org.bytestorm.bitesync.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.bytestorm.bitesync.model.Venue
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val cardGradients = listOf(
    listOf(Color(0xFF667eea), Color(0xFF764ba2)),
    listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
    listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
    listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
    listOf(Color(0xFFfa709a), Color(0xFFfee140)),
    listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb)),
    listOf(Color(0xFFfad0c4), Color(0xFFffd1ff)),
    listOf(Color(0xFF30cfd0), Color(0xFF330867)),
)

@Composable
fun SwipeableCardStack(
    venues: List<Venue>,
    currentIndex: Int,
    onSwipe: (venueId: String, liked: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val visibleCards = venues.drop(currentIndex).take(3)

        visibleCards.asReversed().forEachIndexed { reversedIndex, venue ->
            val stackIndex = visibleCards.size - 1 - reversedIndex

            key(venue.id) {
                SwipeableCard(
                    venue = venue,
                    cardIndex = currentIndex + stackIndex,
                    isTop = stackIndex == 0,
                    stackOffset = stackIndex,
                    onSwiped = { liked -> onSwipe(venue.id, liked) }
                )
            }
        }

        if (currentIndex >= venues.size && venues.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No more venues!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Waiting for a match...", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun SwipeableCard(
    venue: Venue,
    cardIndex: Int,
    isTop: Boolean,
    stackOffset: Int,
    onSwiped: (liked: Boolean) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 300f
    val gradientColors = cardGradients[cardIndex % cardGradients.size]

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.72f)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value + (stackOffset * 20f)
                scaleX = 1f - (stackOffset * 0.05f)
                scaleY = 1f - (stackOffset * 0.05f)
                rotationZ = if (isTop) (offsetX.value / 50f).coerceIn(-15f, 15f) else 0f
                alpha = if (stackOffset >= 3) 0f else 1f - (stackOffset * 0.1f)
            }
            .then(
                if (isTop) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (offsetX.value.absoluteValue > swipeThreshold) {
                                        val liked = offsetX.value > 0
                                        val targetX = if (liked) 1500f else -1500f
                                        offsetX.animateTo(targetX, spring(stiffness = 300f))
                                        onSwiped(liked)
                                    } else {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (8 - stackOffset * 2).coerceAtLeast(0).dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientColors))
            )

            VenueInfoOverlay(venue = venue)

            if (isTop) {
                SwipeIndicators(
                    swipeProgress = (offsetX.value / swipeThreshold).coerceIn(-1f, 1f)
                )
            }
        }
    }
}

@Composable
private fun VenueInfoOverlay(venue: Venue) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    venue.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    venue.rating?.let { rating ->
                        val fullStars = rating.toInt()
                        val hasHalf = (rating - fullStars) >= 0.5f
                        val emptyStars = 5 - fullStars - (if (hasHalf) 1 else 0)
                        val stars = "\u2605".repeat(fullStars) +
                            (if (hasHalf) "\u00BD" else "") +
                            "\u2605".repeat(emptyStars).let {
                                "\u2606".repeat(emptyStars)
                            }
                        Text(stars, color = Color(0xFFFFD700), fontSize = 14.sp, letterSpacing = 1.sp)
                        Text(
                            "${(rating * 10).toInt() / 10.0}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    venue.priceLevel?.let { price ->
                        Text(
                            "$".repeat(price),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (venue.categories.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        venue.categories.take(3).forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    category,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (venue.address.isNotEmpty()) {
                    Text(
                        venue.address,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeIndicators(swipeProgress: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (swipeProgress > 0.1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .graphicsLayer { alpha = swipeProgress }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ) {
                    Text(
                        "LIKE",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    )
                }
            }
        }

        if (swipeProgress < -0.1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .graphicsLayer { alpha = -swipeProgress }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.9f)
                ) {
                    Text(
                        "NOPE",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    )
                }
            }
        }
    }
}
