package com.example.braingames.feature.sequence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.ui.components.ScreenTitle
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// --- Domain ---

private enum class SequencePhase {
    READY, MEMORIZE, ANSWER, RESULT
}

private enum class SequenceDifficulty(val title: String) {
    EASY("Лёгкий"),
    MEDIUM("Средний"),
    HARD("Сложный")
}

private data class DifficultyConfig(
    val startLength: Int,
    val maxLength: Int,
    val baseMemorizePerItemMs: Long,
    val minMemorizeMs: Long,
    val memorizeDecreasePerLevelMs: Long,
    val useColors: Boolean
)

private fun SequenceDifficulty.config(): DifficultyConfig = when (this) {
    SequenceDifficulty.EASY -> DifficultyConfig(
        startLength = 3,
        maxLength = 7,
        baseMemorizePerItemMs = 1400,
        minMemorizeMs = 3000,
        memorizeDecreasePerLevelMs = 150,
        useColors = false
    )

    SequenceDifficulty.MEDIUM -> DifficultyConfig(
        startLength = 4,
        maxLength = 9,
        baseMemorizePerItemMs = 1100,
        minMemorizeMs = 2500,
        memorizeDecreasePerLevelMs = 180,
        useColors = false
    )

    SequenceDifficulty.HARD -> DifficultyConfig(
        startLength = 5,
        maxLength = 10,
        baseMemorizePerItemMs = 950,
        minMemorizeMs = 2000,
        memorizeDecreasePerLevelMs = 200,
        useColors = true
    )
}

private enum class ShapeKind(val label: String) {
    CIRCLE("●"),
    TRIANGLE("▲"),
    STAR("★"),
    HEART("❤"),
    DIAMOND("◆")
}

private data class SequenceItem(
    val id: Int,
    val shape: ShapeKind,
    val color: Color
)

private val pastelPalette = listOf(
    Color(0xFFFFCDD2),
    Color(0xFFFFF9C4),
    Color(0xFFC8E6C9),
    Color(0xFFBBDEFB),
    Color(0xFFD1C4E9),
    Color(0xFFFFE0B2)
)

private val strongPalette = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF9800),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0),
    Color(0xFFFFC107)
)

private fun generateSequence(
    length: Int,
    difficulty: SequenceDifficulty
): List<SequenceItem> {
    val shapes = ShapeKind.values()
    val colors = if (difficulty.config().useColors) strongPalette else pastelPalette
    return List(length) { index ->
        val shape = shapes[index % shapes.size]
        val color = if (difficulty.config().useColors) {
            colors[Random.nextInt(colors.size)]
        } else {
            colors[index % colors.size]
        }
        SequenceItem(id = index, shape = shape, color = color)
    }
}

// --- UI ---

@Composable
fun SequenceGameScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(SequenceDifficulty.EASY) }
    var level by remember(difficulty) { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf(SequencePhase.READY) }

    var sequence by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var shuffled by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var answer by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var memorizeTimeMs by remember { mutableStateOf(0L) }
    var remainingMs by remember { mutableStateOf(0L) }
    var message by remember { mutableStateOf("Выбери уровень сложности и нажми «Старт».") }
    var lastResult by remember { mutableStateOf<String?>(null) }

    fun startNewRound(resetLevel: Boolean = false) {
        if (resetLevel) {
            level = 1
        }
        val cfg = difficulty.config()
        val length = min(cfg.startLength + (level - 1), cfg.maxLength)
        sequence = generateSequence(length, difficulty)
        shuffled = sequence.shuffled()
        answer = emptyList()
        lastResult = null

        val rawMemorize = cfg.baseMemorizePerItemMs * length -
                cfg.memorizeDecreasePerLevelMs * (level - 1)
        memorizeTimeMs = max(cfg.minMemorizeMs, rawMemorize)
        remainingMs = memorizeTimeMs

        phase = SequencePhase.MEMORIZE
        message = "Запомни порядок фигур и их цвета!"
    }

    LaunchedEffect(phase, sequence, memorizeTimeMs) {
        if (phase == SequencePhase.MEMORIZE && sequence.isNotEmpty()) {
            remainingMs = memorizeTimeMs
            while (remainingMs > 0 && phase == SequencePhase.MEMORIZE) {
                delay(200)
                remainingMs = max(0L, remainingMs - 200)
            }
            if (phase == SequencePhase.MEMORIZE) {
                phase = SequencePhase.ANSWER
                message = "Теперь расположи фигурки в правильном порядке."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Запомни последовательность", onBack = onBack)

        // Difficulty selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SequenceDifficulty.values().forEach { diff ->
                val selected = diff == difficulty
                Surface(
                    tonalElevation = if (selected) 4.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            difficulty = diff
                            phase = SequencePhase.READY
                            sequence = emptyList()
                            shuffled = emptyList()
                            answer = emptyList()
                            lastResult = null
                            level = 1
                            message = "Выбран уровень: ${diff.title}. Нажми «Старт»."
                        }
                ) {
                    Text(
                        text = diff.title,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Text(
            text = "Уровень $level · Сложность: ${difficulty.title}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )

        if (phase == SequencePhase.MEMORIZE && memorizeTimeMs > 0) {
            val secondsLeft = (remainingMs / 1000).coerceAtLeast(0)
            Text(
                text = "Время на запоминание: ${secondsLeft}с",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main play area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Target sequence (slots)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (phase == SequencePhase.MEMORIZE)
                        "Запомни порядок слева направо"
                    else
                        "Порядок, который нужно собрать",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val toShow = when (phase) {
                        SequencePhase.MEMORIZE -> sequence
                        SequencePhase.ANSWER, SequencePhase.RESULT -> sequence
                        SequencePhase.READY -> emptyList()
                    }
                    toShow.forEachIndexed { index, item ->
                        SequenceItemView(
                            item = if (phase == SequencePhase.MEMORIZE) item else answer.getOrNull(index),
                            placeholder = phase != SequencePhase.MEMORIZE,
                            index = index
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pool of pieces to select in answer phase
            if (phase == SequencePhase.ANSWER || phase == SequencePhase.RESULT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Нажимай фигурки в правильном порядке",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(shuffled.size) { idx ->
                            val item = shuffled[idx]
                            val used = answer.any { it.id == item.id }
                            SequenceItemButton(
                                item = item,
                                enabled = !used && phase == SequencePhase.ANSWER
                            ) {
                                val newAnswer = answer + item
                                answer = newAnswer
                                if (newAnswer.size == sequence.size) {
                                    val correct = newAnswer.map { it.id } == sequence.map { it.id }
                                    phase = SequencePhase.RESULT
                                    lastResult = if (correct) {
                                        "Отлично! Последовательность собрана правильно."
                                    } else {
                                        "Есть ошибки в порядке. Попробуй ещё раз."
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        lastResult?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
                color = if (it.startsWith("Отлично")) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    startNewRound(resetLevel = true)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Старт")
            }

            Button(
                onClick = {
                    // Следующий уровень только при успехе
                    if (lastResult != null && lastResult!!.startsWith("Отлично")) {
                        level++
                    }
                    startNewRound(resetLevel = false)
                },
                enabled = sequence.isNotEmpty() && phase == SequencePhase.RESULT,
                modifier = Modifier.weight(1f)
            ) {
                Text("Следующий уровень")
            }
        }
    }
}

@Composable
private fun SequenceItemView(
    item: SequenceItem?,
    placeholder: Boolean,
    index: Int
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 2.dp,
                color = if (placeholder) Color.LightGray else item?.color ?: Color.LightGray,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = item?.color ?: Color(0xFFF5F5F5),
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        val text = item?.shape?.label ?: "?"
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SequenceItemButton(
    item: SequenceItem,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = if (enabled) item.color else item.color.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.large
            )
            .border(
                width = 2.dp,
                color = Color.White,
                shape = MaterialTheme.shapes.large
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.shape.label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


