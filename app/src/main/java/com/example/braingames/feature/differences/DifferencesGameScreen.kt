package com.example.braingames.feature.differences

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

// --- Domain / data ---

/** Уровень = одна базовая картинка из drawable с префиксом diff_ */
private data class DiffLevel(val drawableResId: Int)

/** Одно отличие: центр и радиус «попадания» в пикселях картинки */
private data class DifferenceSpot(
    val x: Float,
    val y: Float,
    val touchRadius: Float
)

private const val MIN_DIFFERENCES = 5
private const val MAX_DIFFERENCES = 7
private const val TOUCH_RADIUS_DP = 28f
private const val OUTLINE_DARKEN = 0.55f   // контур темнее фона для чёткости
private const val OUTLINE_WIDTH = 0.12f    // толщина контура в единицах фигуры

/**
 * Загружаем уровни «Найди отличия» из res/drawable.
 * Картинки должны называться с префиксом diff_ (например diff_beach, diff_cat).
 * Отдельно от пазлов (puzzle_), чтобы не было путаницы.
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

/** Берёт цвет картинки в точке (с лёгким усреднением по области), чтобы фигурка сливалась с фоном. */
private fun sampleColorAt(bitmap: Bitmap, x: Int, y: Int): Int {
    val w = bitmap.width
    val h = bitmap.height
    var r = 0; var g = 0; var b = 0; var cnt = 0
    for (dy in -1..1) for (dx in -1..1) {
        val nx = (x + dx).coerceIn(0, w - 1)
        val ny = (y + dy).coerceIn(0, h - 1)
        val p = bitmap.getPixel(nx, ny)
        r += android.graphics.Color.red(p)
        g += android.graphics.Color.green(p)
        b += android.graphics.Color.blue(p)
        cnt++
    }
    return android.graphics.Color.argb(255, r / cnt, g / cnt, b / cnt)
}

/** Темнее цвет для контура — «более чёткое очертание», внутри тот же цвет. */
private fun outlineColor(fillColor: Int): Int {
    val r = (android.graphics.Color.red(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    val g = (android.graphics.Color.green(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    val b = (android.graphics.Color.blue(fillColor) * OUTLINE_DARKEN).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(255, r, g, b)
}

/** Типы фигур: кошка, яйцо, собака и т.д. Разного размера, цвет как у фона + чёткий контур. */
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
 * Создаёт копию базовой картинки и добавляет 5–7 отличий в виде фигурок
 * (кошка, яйцо, собака, звезда, сердце, рыбка, птичка). Цвет заливки — как у фона в этом месте,
 * контур чуть темнее для чёткого очертания.
 */
private fun createModifiedBitmapWithDifferences(
    baseBitmap: Bitmap,
    touchRadiusPx: Float
): Pair<Bitmap, List<DifferenceSpot>> {
    val w = baseBitmap.width
    val h = baseBitmap.height
    val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(mutableBitmap)
    val margin = min(w, h) / 6
    val spots = mutableListOf<DifferenceSpot>()
    val count = Random.nextInt(MIN_DIFFERENCES, MAX_DIFFERENCES + 1)
    val shapes = DifferenceShape.values()

    repeat(count) {
        val x = (margin + Random.nextFloat() * (w - 2 * margin)).toInt().toFloat()
        val y = (margin + Random.nextFloat() * (h - 2 * margin)).toInt().toFloat()
        val sizePx = (min(w, h) * (0.04f + Random.nextFloat() * 0.06f)).coerceIn(18f, 80f)
        val fillColor = sampleColorAt(baseBitmap, x.toInt(), y.toInt())
        val strokeColor = outlineColor(fillColor)
        val shape = shapes.random()
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
            ScreenTitle(title = "Поиск отличий", onBack = onBack)
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

    val (originalBitmap, modifiedBitmap, spots) = remember(currentLevel, touchRadiusPx) {
        val base = BitmapFactory.decodeResource(context.resources, currentLevel.drawableResId)
            ?: error("Cannot decode diff level drawable")
        val (modified, spotsList) = createModifiedBitmapWithDifferences(base, touchRadiusPx)
        Triple(
            base.asImageBitmap(),
            modified.asImageBitmap(),
            spotsList
        )
    }

    var foundIndices by remember(currentLevel) { mutableStateOf(setOf<Int>()) }
    var modifiedImageSize by remember { mutableStateOf(IntSize.Zero) }

    val allFound = foundIndices.size == spots.size
    val hasNextLevel = currentLevelIndex < levels.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(
            title = "Отличия ${currentLevelIndex + 1} из ${levels.size}",
            onBack = onBack
        )

        Text(
            text = "Найди все отличия на правой картинке. Тапай по ним.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Image(
                    bitmap = originalBitmap,
                    contentDescription = "Исходная картинка",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Оригинал",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .onSizeChanged { modifiedImageSize = it }
                    .pointerInput(currentLevel, modifiedImageSize) {
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
                    contentDescription = "Картинка с отличиями",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
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
                    }
                }
                Text(
                    text = "Тапай здесь",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (allFound) {
            Text(
                text = if (hasNextLevel) "Все отличия найдены!" else "Игра пройдена!",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        foundIndices = emptySet()
                        if (hasNextLevel) currentLevelIndex++
                        else currentLevelIndex = 0
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (hasNextLevel) "Следующий уровень" else "Играть сначала")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Выйти в меню")
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
