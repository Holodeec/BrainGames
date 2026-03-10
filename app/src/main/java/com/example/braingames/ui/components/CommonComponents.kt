package com.example.braingames.ui.components


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.braingames.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ScreenTitle(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "← Назад",
            modifier = Modifier
                .clickable { onBack() }
                .padding(4.dp),
            color = Orange,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Orange,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var size: Float,
    var life: Float = 1f
)

@Composable
fun FireworksAnimation(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    containerSize: IntSize
) {
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                val screenWidth = containerSize.width.toFloat()
                val screenHeight = containerSize.height.toFloat()

                if (screenWidth > 0 && screenHeight > 0) {
                    // Создаем новый взрыв каждые 500 мс
                    val centerX = screenWidth / 2
                    val centerY = screenHeight / 2

                    repeat(30) {
                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        val speed = 5f + Random.nextFloat() * 10f
                        val color = Color(
                            red = 0.7f + Random.nextFloat() * 0.3f,
                            green = 0.5f + Random.nextFloat() * 0.5f,
                            blue = 0.2f + Random.nextFloat() * 0.8f
                        )

                        particles.add(
                            Particle(
                                x = centerX,
                                y = centerY,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed,
                                color = color,
                                size = 8f + Random.nextFloat() * 10f
                            )
                        )
                    }

                    delay(500)
                } else {
                    delay(500)
                }
            }
        } else {
            particles.clear()
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                delay(16)

                val particlesToRemove = mutableListOf<Particle>()
                particles.forEach { particle ->
                    particle.vy += 0.2f
                    particle.x += particle.vx
                    particle.y += particle.vy
                    particle.life -= 0.02f
                    particle.size *= 0.98f
                    particle.vx *= 0.99f
                    particle.vy *= 0.99f

                    if (particle.life <= 0.01f || particle.y > containerSize.height + 200) {
                        particlesToRemove.add(particle)
                    }
                }
                particles.removeAll(particlesToRemove)
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color.copy(alpha = particle.life),
                radius = particle.size * particle.life,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}

