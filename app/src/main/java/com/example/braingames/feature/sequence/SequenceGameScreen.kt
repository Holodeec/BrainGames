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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.ui.components.FireworksAnimation
import com.example.braingames.ui.components.ScreenTitle
import com.example.braingames.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private enum class SequencePhase { READY, MEMORIZE, ANSWER, RESULT }
private enum class SequenceDifficulty(val title: String, val description: String) {
    EASY("Лёгкий", "3-7 предметов\n3 сек на запоминание"),
    MEDIUM("Средний", "4-9 предметов\n2.5 сек на запоминание\n+ цвета"),
    HARD("Сложный", "5-10 предметов\n2 сек на запоминание\n+ разные цвета")
}
private enum class ShapeKind(val label: String) { CIRCLE("●"), TRIANGLE("▲"), STAR("★"), HEART("❤"), DIAMOND("◆") }

private data class DifficultyConfig(
    val startLength: Int,
    val maxLength: Int,
    val baseMemorizePerItemMs: Long,
    val minMemorizeMs: Long,
    val memorizeDecreasePerLevelMs: Long,
    val useColors: Boolean
)

private data class SequenceItem(val id: Int, val shape: ShapeKind, val color: Color)

private fun SequenceDifficulty.config(): DifficultyConfig = when (this) {
    SequenceDifficulty.EASY -> DifficultyConfig(3, 7, 1400, 3000, 150, false)
    SequenceDifficulty.MEDIUM -> DifficultyConfig(4, 9, 1100, 2500, 180, true)
    SequenceDifficulty.HARD -> DifficultyConfig(5, 10, 950, 2000, 200, true)
}

private val pastelPalette = listOf(
    Color(0xFFFFCDD2), Color(0xFFFFF9C4), Color(0xFFC8E6C9),
    Color(0xFFBBDEFB), Color(0xFFD1C4E9), Color(0xFFFFE0B2)
)

private val strongPalette = listOf(
    Color(0xFFE91E63), Color(0xFFFF9800), Color(0xFF4CAF50),
    Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFFC107)
)

private fun generateSequence(length: Int, difficulty: SequenceDifficulty): List<SequenceItem> {
    val shapes = ShapeKind.values()
    val colors = if (difficulty.config().useColors) strongPalette else pastelPalette
    return List(length) { index ->
        SequenceItem(
            id = index,
            shape = shapes[index % shapes.size],
            color = if (difficulty.config().useColors) colors[Random.nextInt(colors.size)] else colors[index % colors.size]
        )
    }
}

@Composable
fun SequenceGameScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(SequenceDifficulty.EASY) }
    var level by remember(difficulty) { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf(SequencePhase.READY) }
    var sequence by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var shuffled by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var answer by remember { mutableStateOf<List<SequenceItem>>(emptyList()) }
    var message by remember { mutableStateOf("Выбери уровень сложности") }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var sequenceImageSize by remember { mutableStateOf(IntSize.Zero) }
    var progress by remember { mutableFloatStateOf(1f) }

    // Исправлено: используем mutableLongStateOf
    var timeLeftMs by remember { mutableLongStateOf(0L) }
    var totalTimeMs by remember { mutableLongStateOf(0L) }

    fun startNewRound(resetLevel: Boolean = false) {
        if (resetLevel) level = 1
        val cfg = difficulty.config()
        val length = min(cfg.startLength + (level - 1), cfg.maxLength)
        sequence = generateSequence(length, difficulty)
        shuffled = sequence.shuffled()
        answer = emptyList()
        lastResult = null

        val memorizeTime = max(cfg.minMemorizeMs, cfg.baseMemorizePerItemMs * length - cfg.memorizeDecreasePerLevelMs * (level - 1))
        totalTimeMs = memorizeTime
        timeLeftMs = memorizeTime
        progress = 1f

        phase = SequencePhase.MEMORIZE
        message = "Запомни порядок!"
    }

    LaunchedEffect(phase) {
        if (phase == SequencePhase.MEMORIZE) {
            while (timeLeftMs > 0 && phase == SequencePhase.MEMORIZE) {
                delay(50)
                timeLeftMs = max(0, timeLeftMs - 50)
                progress = timeLeftMs.toFloat() / totalTimeMs.toFloat()
            }
            if (phase == SequencePhase.MEMORIZE) {
                phase = SequencePhase.ANSWER
                message = "Выбери фигурки по порядку"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Запомни последовательность", onBack = onBack)

        // Информация о сложности
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Orange.copy(alpha = 0.1f))
        ) {
            Text(
                text = difficulty.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }

        // Выбор сложности (только в состоянии READY)
        if (phase == SequencePhase.READY) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SequenceDifficulty.values().forEach { diff ->
                    val selected = diff == difficulty
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                difficulty = diff
                                message = "Выбран уровень: ${diff.title}"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Orange else Color.LightGray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = diff.title,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        Text(
            text = "Уровень $level",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            color = Orange
        )

        if (phase == SequencePhase.MEMORIZE) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp)
                    .align(Alignment.CenterHorizontally),
                color = Orange,
                trackColor = Color.LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { sequenceImageSize = it }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Целевая последовательность
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (phase) {
                            SequencePhase.MEMORIZE -> "Запомни порядок"
                            SequencePhase.ANSWER, SequencePhase.RESULT -> "Собери так:"
                            SequencePhase.READY -> "Нажми СТАРТ"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val toShow = when (phase) {
                            SequencePhase.MEMORIZE -> sequence
                            SequencePhase.ANSWER, SequencePhase.RESULT -> sequence
                            SequencePhase.READY -> emptyList()
                        }
                        if (toShow.isEmpty()) {
                            Text(
                                text = "———",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.LightGray
                            )
                        } else {
                            toShow.forEachIndexed { index, item ->
                                SequenceItemView(
                                    item = if (phase == SequencePhase.MEMORIZE || phase == SequencePhase.READY) item else answer.getOrNull(index),
                                    placeholder = phase == SequencePhase.ANSWER && answer.getOrNull(index) == null,
                                    index = index
                                )
                            }
                        }
                    }
                }

                // Поле для выбора (только в ANSWER)
                if (phase == SequencePhase.ANSWER) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Выбери следующую:",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            items(shuffled.size) { idx ->
                                val item = shuffled[idx]
                                val used = answer.any { it.id == item.id }
                                SequenceItemButton(
                                    item = item,
                                    enabled = !used
                                ) {
                                    val newAnswer = answer + item
                                    answer = newAnswer
                                    if (newAnswer.size == sequence.size) {
                                        val correct = newAnswer.map { it.id } == sequence.map { it.id }
                                        phase = SequencePhase.RESULT
                                        lastResult = if (correct) "Правильно! Молодец!"
                                        else "Ошибка. Попробуй ещё раз"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FireworksAnimation(
                modifier = Modifier.fillMaxSize(),
                isActive = phase == SequencePhase.RESULT && lastResult?.startsWith("Правильно") == true,
                containerSize = sequenceImageSize
            )
        }

        lastResult?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = if (it.startsWith("Правильно")) Orange else MaterialTheme.colorScheme.error
            )
        }

        // Кнопки
        if (phase == SequencePhase.READY) {
            Button(
                onClick = { startNewRound(resetLevel = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Text("СТАРТ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (phase == SequencePhase.RESULT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (lastResult?.startsWith("Правильно") == true) level++
                        startNewRound(resetLevel = false)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    enabled = lastResult?.startsWith("Правильно") == true
                ) {
                    Text("Следующий уровень")
                }
                Button(
                    onClick = {
                        phase = SequencePhase.READY
                        message = "Выбери уровень сложности"
                        lastResult = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Заново")
                }
            }
        }
    }
}

@Composable
private fun SequenceItemView(item: SequenceItem?, placeholder: Boolean, index: Int) {
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
                color = if (placeholder) Color.White else item?.color ?: Color(0xFFF5F5F5),
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (placeholder) "?" else item?.shape?.label ?: "?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (placeholder) Color.LightGray else Color.Black
        )
    }
}

@Composable
private fun SequenceItemButton(item: SequenceItem, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = if (enabled) item.color else item.color.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.large
            )
            .border(width = 2.dp, color = Color.White, shape = MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.shape.label,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}