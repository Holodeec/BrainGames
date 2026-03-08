package com.example.braingames.feature.differences

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.R
import com.example.braingames.ui.components.ScreenTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// --- Domain / data ---

/** Уровень = одна базовая картинка из drawable с префиксом diff_ */
private data class DiffLevel(val drawableResId: Int)

/** Один предмет: центр и радиус «попадания» в пикселях картинки */
private data class DifferenceSpot(
    val x: Float,
    val y: Float,
    val touchRadius: Float
)

private const val MIN_DIFFERENCES = 5
private const val MAX_DIFFERENCES = 7
private const val TOUCH_RADIUS_DP = 28f
private const val MIN_DISTANCE_MULTIPLIER = 4f
private const val OUTLINE_DARKEN = 0.55f
private const val OUTLINE_WIDTH = 0.12f
private const val EDGE_MARGIN_FACTOR = 0.1f

// Класс для частицы фейерверка с физикой
private data class FireworkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var size: Float,
    var alpha: Float = 1f,
    var life: Float = 1f,
    val maxLife: Float = 2f + Random.nextFloat() * 2f
)

// Класс для ракеты
private data class Rocket(
    var x: Float,
    var y: Float,
    var targetY: Float,
    val color: Color,
    var progress: Float = 0f,
    val explosionParticles: MutableList<FireworkParticle> = mutableListOf(),
    var hasExploded: Boolean = false,
    var explosionTime: Float = 0f
)

/**
 * Загружаем уровни «Найди предметы» из res/drawable.
 * Картинки должны называться с префиксом diff_ (например diff_beach, diff_cat).
 */
private fun loadDiffLevels(): List<DiffLevel> {
    val fields = R.drawable::class.java.fields
    return fields
        .filter { it.name.startsWith("diff_") }
        .sortedBy { it.name }
        .mapNotNull { field ->
            try {
                DiffLevel(field.getInt(null))
            } catch (_: Exception) {
                null
            }
        }
}

/** Берёт цвет картинки в точке */
private fun sampleColorAt(bitmap: Bitmap, x: Float, y: Float): Int {
    val ix = x.toInt().coerceIn(0, bitmap.width - 1)
    val iy = y.toInt().coerceIn(0, bitmap.height - 1)

    var r = 0;
    var g = 0;
    var b = 0;
    var cnt = 0
    for (dy in -1..1) for (dx in -1..1) {
        val nx = (ix + dx).coerceIn(0, bitmap.width - 1)
        val ny = (iy + dy).coerceIn(0, bitmap.height - 1)
        val p = bitmap.getPixel(nx, ny)
        r += android.graphics.Color.red(p)
        g += android.graphics.Color.green(p)
        b += android.graphics.Color.blue(p)
        cnt++
    }
    return android.graphics.Color.argb(255, r / cnt, g / cnt, b / cnt)
}

/** Темнее цвет для контура */
private fun outlineColor(fillColor: Int): Int {
    val r = (android.graphics.Color.red(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    val g = (android.graphics.Color.green(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    val b = (android.graphics.Color.blue(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(255, r, g, b)
}

/** Типы фигур */
private enum class DifferenceShape {
    EGG, CAT, DOG, STAR, HEART, FISH, BIRD
}

private fun Path.eggPath() {
    reset()
    addOval(RectF(-0.55f, -0.75f, 0.55f, 0.75f), Path.Direction.CW)
}

private fun Path.catPath() {
    reset()
    addCircle(0f, 0f, 0.5f, Path.Direction.CW)
    moveTo(-0.5f, -0.35f); lineTo(-0.88f, -0.72f); lineTo(-0.35f, -0.45f); close()
    moveTo(0.5f, -0.35f); lineTo(0.88f, -0.72f); lineTo(0.35f, -0.45f); close()
}

private fun Path.dogPath() {
    reset()
    addCircle(0f, 0f, 0.5f, Path.Direction.CW)
    moveTo(-0.48f, 0.02f); lineTo(-0.75f, 0.45f); lineTo(-0.2f, 0.15f); close()
    moveTo(0.48f, 0.02f); lineTo(0.75f, 0.45f); lineTo(0.2f, 0.15f); close()
}

private fun Path.starPath() {
    reset()
    val outer = 0.6f
    val inner = 0.28f
    for (i in 0 until 10) {
        val angle = (i * 36 - 90) * PI / 180
        val r = if (i % 2 == 0) outer else inner
        val px = (r * cos(angle)).toFloat()
        val py = (r * sin(angle)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

private fun Path.heartPath() {
    reset()
    moveTo(0f, 0.25f)
    cubicTo(0.5f, -0.4f, 0.9f, 0.15f, 0f, 0.65f)
    cubicTo(-0.9f, 0.15f, -0.5f, -0.4f, 0f, 0.25f)
    close()
}

private fun Path.fishPath() {
    reset()
    addOval(RectF(-0.55f, -0.28f, 0.55f, 0.28f), Path.Direction.CW)
    moveTo(0.5f, 0f); lineTo(0.92f, -0.28f); lineTo(0.92f, 0.28f); close()
}

private fun Path.birdPath() {
    reset()
    addOval(RectF(-0.5f, -0.22f, 0.5f, 0.22f), Path.Direction.CW)
    moveTo(0.45f, -0.1f); lineTo(0.85f, -0.35f); lineTo(0.85f, 0.1f); close()
    moveTo(0.45f, 0.1f); lineTo(0.85f, 0.35f); lineTo(0.85f, -0.1f); close()
}

private fun buildPathFor(shape: DifferenceShape): Path {
    val p = Path()
    when (shape) {
        DifferenceShape.EGG -> p.eggPath()
        DifferenceShape.CAT -> p.catPath()
        DifferenceShape.DOG -> p.dogPath()
        DifferenceShape.STAR -> p.starPath()
        DifferenceShape.HEART -> p.heartPath()
        DifferenceShape.FISH -> p.fishPath()
        DifferenceShape.BIRD -> p.birdPath()
    }
    return p
}

/**
 * Создаёт картинку с предметами
 */
private fun createModifiedBitmapWithDifferences(
    baseBitmap: Bitmap,
    touchRadiusPx: Float,
    randomSeed: Int
): Pair<Bitmap, List<DifferenceSpot>> {
    val w = baseBitmap.width.toFloat()
    val h = baseBitmap.height.toFloat()
    val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(mutableBitmap)

    val margin = min(w, h) * EDGE_MARGIN_FACTOR

    val spots = mutableListOf<DifferenceSpot>()
    val count = Random(randomSeed).nextInt(MIN_DIFFERENCES, MAX_DIFFERENCES + 1)
    val shapes = DifferenceShape.values()
    val random = Random(randomSeed)

    val minDistance = touchRadiusPx * MIN_DISTANCE_MULTIPLIER
    var attempts = 0
    val maxAttempts = 2000

    while (spots.size < count && attempts < maxAttempts) {
        attempts++

        val x = margin + random.nextFloat() * (w - 2 * margin)
        val y = margin + random.nextFloat() * (h - 2 * margin)

        var tooClose = false
        for (spot in spots) {
            val distance = hypot(x - spot.x, y - spot.y)
            if (distance < minDistance) {
                tooClose = true
                break
            }
        }

        if (tooClose) continue

        val sizePx = (min(w, h) * (0.03f + random.nextFloat() * 0.07f)).coerceIn(15f, 70f)
        val fillColor = sampleColorAt(baseBitmap, x, y)
        val strokeColor = outlineColor(fillColor)
        val shape = shapes.random(random)
        val path = buildPathFor(shape)

        canvas.save()
        canvas.translate(x, y)
        canvas.scale(sizePx, sizePx)

        val fillPaint = Paint().apply {
            setColor(fillColor)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, fillPaint)

        val strokePaint = Paint().apply {
            setColor(strokeColor)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = OUTLINE_WIDTH
        }
        canvas.drawPath(path, strokePaint)
        canvas.restore()

        spots.add(DifferenceSpot(x, y, touchRadiusPx))
    }

    return mutableBitmap to spots
}

// --- UI ---

@Composable
fun DifferencesGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val touchRadiusPx = (TOUCH_RADIUS_DP * density).coerceAtLeast(20f)

    val levels = remember { loadDiffLevels() }

    if (levels.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScreenTitle(title = "Найди предметы", onBack = onBack)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет картинок для игры.\nДобавь в res/drawable файлы с префиксом diff_ (например diff_beach.png).",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Выйти в меню") }
        }
        return
    }

    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val currentLevel = levels[currentLevelIndex]

    var gameSeed by remember(currentLevelIndex) { mutableIntStateOf(Random.nextInt()) }

    val (modifiedBitmap, spots) = remember(currentLevel, gameSeed, touchRadiusPx) {
        val base = BitmapFactory.decodeResource(context.resources, currentLevel.drawableResId)
            ?: error("Cannot decode diff level drawable")
        val (modified, spotsList) = createModifiedBitmapWithDifferences(
            base,
            touchRadiusPx,
            gameSeed
        )
        Pair(modified.asImageBitmap(), spotsList)
    }

    var foundIndices by remember(currentLevel, gameSeed) { mutableStateOf(setOf<Int>()) }
    var modifiedImageSize by remember { mutableStateOf(IntSize.Zero) }

    val allFound = foundIndices.size == spots.size
    val hasNextLevel = currentLevelIndex < levels.lastIndex

    // Состояние для живой анимации фейерверков
    val rockets = remember { mutableStateListOf<Rocket>() }
    val particles = remember { mutableStateListOf<FireworkParticle>() }
    val scope = rememberCoroutineScope()
    val animationTime = remember { Animatable(0f) }

    // Запускаем анимацию когда все предметы найдены
    LaunchedEffect(allFound) {
        if (allFound) {
            // Запускаем бесконечную анимацию
            while (true) {
                val screenWidth = modifiedImageSize.width.toFloat()
                val screenHeight = modifiedImageSize.height.toFloat()

                if (screenWidth > 0 && screenHeight > 0) {
                    // Запускаем несколько ракет с задержкой
                    launch {
                        repeat(5) { i ->
                            delay(i * 800L)

                            val startX = 100f + Random.nextFloat() * (screenWidth - 200f)
                            val color = Color(
                                red = 0.7f + Random.nextFloat() * 0.3f,
                                green = 0.5f + Random.nextFloat() * 0.5f,
                                blue = 0.2f + Random.nextFloat() * 0.8f,
                                alpha = 1f
                            )

                            rockets.add(
                                Rocket(
                                    x = startX,
                                    y = screenHeight + 50f,
                                    targetY = 100f + Random.nextFloat() * (screenHeight * 0.4f),
                                    color = color
                                )
                            )
                        }
                    }

                    // Каждые 3 секунды запускаем новый залп
                    delay(3000)
                } else {
                    delay(500)
                }
            }
        } else {
            rockets.clear()
            particles.clear()
        }
    }

    // Анимационный loop для обновления физики
    LaunchedEffect(allFound) {
        if (allFound) {
            while (true) {
                delay(16) // ~60 FPS

                // Обновляем ракеты
                val rocketsToRemove = mutableListOf<Rocket>()
                rockets.forEach { rocket ->
                    if (!rocket.hasExploded) {
                        // Движение ракеты вверх
                        rocket.progress += 0.02f
                        rocket.y = rocket.y - 8f

                        // Проверяем взрыв
                        if (rocket.y <= rocket.targetY || rocket.progress >= 1f) {
                            rocket.hasExploded = true
                            rocket.explosionTime = 0f

                            // Создаем частицы взрыва
                            val explosionX = rocket.x
                            val explosionY = rocket.y

                            repeat(40) {
                                val angle = Random.nextFloat() * 2f * PI.toFloat()
                                val speed = 5f + Random.nextFloat() * 10f
                                val colorVariation = 0.3f

                                particles.add(
                                    FireworkParticle(
                                        x = explosionX,
                                        y = explosionY,
                                        vx = cos(angle) * speed,
                                        vy = sin(angle) * speed - 2f,
                                        color = Color(
                                            red = (rocket.color.red + (Random.nextFloat() - 0.5f) * colorVariation).coerceIn(
                                                0.2f,
                                                1f
                                            ),
                                            green = (rocket.color.green + (Random.nextFloat() - 0.5f) * colorVariation).coerceIn(
                                                0.2f,
                                                1f
                                            ),
                                            blue = (rocket.color.blue + (Random.nextFloat() - 0.5f) * colorVariation).coerceIn(
                                                0.2f,
                                                1f
                                            ),
                                            alpha = 1f
                                        ),
                                        size = 8f + Random.nextFloat() * 10f
                                    )
                                )
                            }

                            rocketsToRemove.add(rocket)
                        }
                    }
                }
                rockets.removeAll(rocketsToRemove)

                // Обновляем частицы с физикой
                val particlesToRemove = mutableListOf<FireworkParticle>()
                particles.forEach { particle ->
                    // Гравитация
                    particle.vy += 0.2f

                    // Обновляем позицию
                    particle.x += particle.vx
                    particle.y += particle.vy

                    // Затухание
                    particle.life -= 0.01f
                    particle.alpha = particle.life
                    particle.size *= 0.99f

                    // Сопротивление воздуха
                    particle.vx *= 0.99f
                    particle.vy *= 0.99f

                    // Удаляем мертвые частицы
                    if (particle.life <= 0.01f || particle.y > modifiedImageSize.height + 200) {
                        particlesToRemove.add(particle)
                    }
                }
                particles.removeAll(particlesToRemove)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(
            title = "Уровень ${currentLevelIndex + 1} из ${levels.size}",
            onBack = onBack
        )

        Text(
            text = "Найди все предметы на картинке. Тапай по ним.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .onSizeChanged { modifiedImageSize = it }
                .pointerInput(currentLevel, modifiedImageSize, gameSeed) {
                    if (modifiedImageSize.width == 0 || modifiedImageSize.height == 0) return@pointerInput
                    val bmW = modifiedBitmap.width.toFloat()
                    val bmH = modifiedBitmap.height.toFloat()
                    val boxW = modifiedImageSize.width.toFloat()
                    val boxH = modifiedImageSize.height.toFloat()
                    val scale = min(boxW / bmW, boxH / bmH)
                    val contentW = bmW * scale
                    val contentH = bmH * scale
                    val contentLeft = (boxW - contentW) / 2
                    val contentTop = (boxH - contentH) / 2

                    detectTapGestures { offset ->
                        val bx = (offset.x - contentLeft) / contentW * bmW
                        val by = (offset.y - contentTop) / contentH * bmH
                        if (bx < 0 || bx >= bmW || by < 0 || by >= bmH) return@detectTapGestures
                        spots.forEachIndexed { index, spot ->
                            if (index in foundIndices) return@forEachIndexed
                            if (hypot(bx - spot.x, by - spot.y) <= spot.touchRadius) {
                                foundIndices = foundIndices + index
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            Image(
                bitmap = modifiedBitmap,
                contentDescription = "Картинка с предметами",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Рисуем зеленые кружки на найденных предметах и живую анимацию
            if (modifiedImageSize.width > 0 && modifiedImageSize.height > 0) {
                val bmW = modifiedBitmap.width.toFloat()
                val bmH = modifiedBitmap.height.toFloat()
                val boxW = modifiedImageSize.width.toFloat()
                val boxH = modifiedImageSize.height.toFloat()
                val scale = min(boxW / bmW, boxH / bmH)
                val contentW = bmW * scale
                val contentH = bmH * scale
                val contentLeft = (boxW - contentW) / 2
                val contentTop = (boxH - contentH) / 2

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Рисуем найденные предметы
                    foundIndices.forEach { index ->
                        val spot = spots[index]
                        val cx = contentLeft + (spot.x / bmW) * contentW
                        val cy = contentTop + (spot.y / bmH) * contentH
                        val r = (spot.touchRadius / bmW * contentW).coerceAtLeast(8f)
                        drawCircle(
                            color = Color.Green.copy(alpha = 0.5f),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    }

                    // Рисуем живую анимацию фейерверков
                    if (allFound) {
                        // Рисуем ракеты
                        rockets.forEach { rocket ->
                            if (!rocket.hasExploded) {
                                // Ракета
                                val rocketX = contentLeft + (rocket.x / boxW) * contentW
                                val rocketY = contentTop + (rocket.y / boxH) * contentH

                                // Хвост ракеты
                                drawCircle(
                                    color = rocket.color.copy(alpha = 0.7f),
                                    radius = 6f,
                                    center = Offset(rocketX, rocketY)
                                )

                                // Искры от хвоста
                                repeat(3) {
                                    val offsetX = (Random.nextFloat() - 0.5f) * 10f
                                    val offsetY = 10f + Random.nextFloat() * 10f
                                    drawCircle(
                                        color = Color.Yellow.copy(alpha = 0.5f),
                                        radius = 3f,
                                        center = Offset(rocketX + offsetX, rocketY + offsetY)
                                    )
                                }
                            }
                        }

                        // Рисуем частицы взрывов
                        particles.forEach { particle ->
                            val px = contentLeft + (particle.x / boxW) * contentW
                            val py = contentTop + (particle.y / boxH) * contentH

                            if (px in 0f..boxW && py in 0f..boxH + 200) {
                                // Основная частица
                                drawCircle(
                                    color = particle.color.copy(alpha = particle.alpha * 0.8f),
                                    radius = particle.size * (particle.life + 0.5f),
                                    center = Offset(px, py)
                                )

                                // Свечение
                                drawCircle(
                                    color = particle.color.copy(alpha = particle.alpha * 0.3f),
                                    radius = particle.size * 2f * (particle.life + 0.3f),
                                    center = Offset(px, py)
                                )

                                // Искры (маленькие частицы вокруг)
                                if (particle.life > 0.5f) {
                                    repeat(2) {
                                        val sparkX = px + (Random.nextFloat() - 0.5f) * 15f
                                        val sparkY = py + (Random.nextFloat() - 0.5f) * 15f
                                        drawCircle(
                                            color = Color.White.copy(alpha = particle.alpha * 0.4f),
                                            radius = 2f,
                                            center = Offset(sparkX, sparkY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Картинка",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp),
                fontSize = 12.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (allFound) {
            Text(
                text = if (hasNextLevel) "✨ Все предметы найдены! ✨" else "🎉 Игра пройдена! 🎉",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            // Кнопки внизу по середине
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        foundIndices = emptySet()
                        rockets.clear()
                        particles.clear()
                        if (hasNextLevel) {
                            currentLevelIndex++
                        } else {
                            currentLevelIndex = 0
                            gameSeed = Random.nextInt()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(if (hasNextLevel) "Следующий уровень →" else "Играть сначала")
                }

                Button(
                    onClick = {
                        rockets.clear()
                        particles.clear()
                        onBack()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Выйти")
                }
            }
        } else {
            Text(
                text = "Найдено: ${foundIndices.size} из ${spots.size}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}