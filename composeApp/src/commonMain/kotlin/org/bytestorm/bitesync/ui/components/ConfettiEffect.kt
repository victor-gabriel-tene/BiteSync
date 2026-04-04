package org.bytestorm.bitesync.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

private val confettiColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF4D96FF), Color(0xFFFF8E53), Color(0xFFE84393),
    Color(0xFF00CEC9), Color(0xFFFDAA5E)
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.004f,
                velocityY = Random.nextFloat() * 0.006f + 0.002f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = Random.nextFloat() * 10f + 4f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 6f
            )
        }
    }

    val time by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val animatedY = (particle.y + time * particle.velocityY) % 1.3f
            val animatedX = particle.x +
                sin((time * 0.02f + particle.rotation).toDouble()).toFloat() * 0.05f +
                time * particle.velocityX
            val normalizedX = ((animatedX % 1f) + 1f) % 1f

            val px = normalizedX * size.width
            val py = animatedY * size.height
            val rot = particle.rotation + time * particle.rotationSpeed

            rotate(rot, pivot = Offset(px, py)) {
                drawRect(
                    color = particle.color,
                    topLeft = Offset(px - particle.size / 2, py - particle.size / 2),
                    size = Size(particle.size, particle.size * 0.6f)
                )
            }
        }
    }
}
