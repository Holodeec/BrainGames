package com.example.braingames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.ui.BrainGamesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BrainGamesApp() }
    }
}

private enum class GameScreen {
    MENU,
    PUZZLE,
    DIFFERENCES,
    SEQUENCE,
    LOGIC
}

@Composable
fun BrainGamesApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentScreen by remember { mutableStateOf(GameScreen.MENU) }

            when (currentScreen) {
                GameScreen.MENU -> MainMenu(
                    onOpenPuzzle = { currentScreen = GameScreen.PUZZLE },
                    onOpenDifferences = { currentScreen = GameScreen.DIFFERENCES },
                    onOpenSequence = { currentScreen = GameScreen.SEQUENCE },
                    onOpenLogic = { currentScreen = GameScreen.LOGIC }
                )

                GameScreen.PUZZLE -> PuzzleGame(onBack = { currentScreen = GameScreen.MENU })
                GameScreen.DIFFERENCES -> DifferencesGame(onBack = { currentScreen = GameScreen.MENU })
                GameScreen.SEQUENCE -> SequenceGame(onBack = { currentScreen = GameScreen.MENU })
                GameScreen.LOGIC -> LogicGame(onBack = { currentScreen = GameScreen.MENU })
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, onBack: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "← Назад",
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}

// Главный экран-меню
@Composable
fun MainMenu(
    onOpenPuzzle: () -> Unit,
    onOpenDifferences: () -> Unit,
    onOpenSequence: () -> Unit,
    onOpenLogic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Brain Games",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Выбери игру, чтобы потренировать мозг!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuButton("Пазлы из картинок", onOpenPuzzle)
            MenuButton("Поиск отличий", onOpenDifferences)
            MenuButton("Запоминание последовательности", onOpenSequence)
            MenuButton("Логические задания", onOpenLogic)
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = text, fontSize = 18.sp)
    }
}

// 1. Пазлы: простая "сборка" квадратиков по номерам
@Composable
fun PuzzleGame(onBack: () -> Unit) {
    var tiles by remember { mutableStateOf((1..9).shuffled()) }
    var showWinDialog by remember { mutableStateOf(false) }

    val isSolved = tiles == (1..9).toList()

    LaunchedEffect(isSolved) {
        if (isSolved) {
            showWinDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Пазл: собери картинку", onBack = onBack)

        Text(
            text = "Нажимай на квадраты, чтобы расставить их по порядку 1–9.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tiles.size) { index ->
                val value = tiles[index]
                Card(
                    modifier = Modifier
                        .size(90.dp)
                        .clickable(enabled = !isSolved) {
                            // простейший алгоритм: при клике меняем местами с соседним элементом
                            if (index < tiles.lastIndex) {
                                val mutable = tiles.toMutableList()
                                val tmp = mutable[index]
                                mutable[index] = mutable[index + 1]
                                mutable[index + 1] = tmp
                                tiles = mutable
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFBBDEFB)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = value.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                tiles = (1..9).shuffled()
                showWinDialog = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Перемешать")
        }
    }

    if (showWinDialog && isSolved) {
        AlertDialog(
            onDismissRequest = { /* Не закрываем по тапу вне окна, чтобы ребёнок точно сделал выбор */ },
            title = {
                Text(text = "Пазл собран!")
            },
            text = {
                Text(text = "Молодец! Ты собрал картинку. Что дальше?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Продолжить — новый раунд
                        tiles = (1..9).shuffled()
                        showWinDialog = false
                    }
                ) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWinDialog = false
                        onBack()
                    }
                ) {
                    Text("Выйти в меню")
                }
            }
        )
    }
}

// 2. Поиск отличий: упрощённый вариант – выделить все "отличающиеся" квадраты
@Composable
fun DifferencesGame(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Поиск отличий", onBack = onBack)

        Text(
            text = "Найди все отличающиеся квадраты (отмечены другим цветом).",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4 из 12 клеток будут "отличиями"
        val total = 12
        val differenceIndices = remember { (0 until total).shuffled().take(4).toSet() }
        var found by remember { mutableStateOf(setOf<Int>()) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(total) { index ->
                val isDifference = differenceIndices.contains(index)
                val isFound = found.contains(index)

                Card(
                    modifier = Modifier
                        .size(70.dp)
                        .clickable {
                            if (isDifference && !isFound) {
                                found = found + index
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isFound -> Color(0xFFC8E6C9)
                            isDifference -> Color(0xFFFFCDD2)
                            else -> Color(0xFFE3F2FD)
                        }
                    )
                ) {}
            }
        }

        val allFound = found.size == differenceIndices.size
        Text(
            text = if (allFound) "Молодец! Все отличия найдены." else "Найдено отличий: ${found.size} из ${differenceIndices.size}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

// 3. Запоминание последовательности (аналог Simon Says)
@Composable
fun SequenceGame(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Запомни последовательность", onBack = onBack)

        var sequence by remember { mutableStateOf(listOf<Int>()) }
        var playerInput by remember { mutableStateOf(listOf<Int>()) }
        var isShowingSequence by remember { mutableStateOf(false) }
        var currentHighlight by remember { mutableIntStateOf(-1) }
        var message by remember { mutableStateOf("Нажми «Старт», чтобы начать.") }

        fun generateNextStep() {
            sequence = sequence + (0..3).random()
            playerInput = emptyList()
        }

        LaunchedEffect(sequence, isShowingSequence) {
            if (isShowingSequence && sequence.isNotEmpty()) {
                for ((index, value) in sequence.withIndex()) {
                    currentHighlight = value
                    message = "Смотри: шаг ${index + 1}/${sequence.size}"
                    kotlinx.coroutines.delay(600)
                    currentHighlight = -1
                    kotlinx.coroutines.delay(250)
                }
                message = "Твоя очередь! Повтори последовательность."
                isShowingSequence = false
            }
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        val colors = listOf(
            Color(0xFFFFCDD2),
            Color(0xFFC8E6C9),
            Color(0xFFBBDEFB),
            Color(0xFFFFF9C4)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 2x2 цветные кнопки
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) { index ->
                    val baseColor = colors[index]
                    val highlighted = currentHighlight == index

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = if (highlighted) baseColor.copy(alpha = 1f) else baseColor.copy(alpha = 0.6f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .clickable(enabled = !isShowingSequence && sequence.isNotEmpty()) {
                                if (isShowingSequence || sequence.isEmpty()) return@clickable

                                val newInput = playerInput + index
                                playerInput = newInput

                                val position = newInput.lastIndex
                                if (sequence[position] != index) {
                                    message = "Неверно :( Попробуй снова!"
                                    sequence = emptyList()
                                    playerInput = emptyList()
                                    return@clickable
                                }

                                if (newInput.size == sequence.size) {
                                    message = "Отлично! Длина последовательности: ${sequence.size}. Нажми «Следующий уровень»."
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    sequence = emptyList()
                    playerInput = emptyList()
                    generateNextStep()
                    isShowingSequence = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Старт")
            }

            Button(
                onClick = {
                    if (sequence.isNotEmpty() && playerInput.size == sequence.size) {
                        generateNextStep()
                        isShowingSequence = true
                    }
                },
                enabled = sequence.isNotEmpty() && playerInput.size == sequence.size,
                modifier = Modifier.weight(1f)
            ) {
                Text("Следующий уровень")
            }
        }
    }
}

// 4. Логические задания – простые задачки вида "выбери правильный порядок"
data class LogicTask(
    val items: List<String>,
    val correctOrder: List<Int>
)

@Composable
fun LogicGame(onBack: () -> Unit) {
    val tasks = listOf(
        LogicTask(
            items = listOf("Утро", "Ночь", "День", "Вечер"),
            correctOrder = listOf(1, 3, 4, 2) // Можем задать порядок по смыслу
        ),
        LogicTask(
            items = listOf("Семя", "Цветок", "Росток", "Почка"),
            correctOrder = listOf(1, 3, 4, 2)
        )
    )

    var currentIndex by remember { mutableIntStateOf(0) }
    var shuffledOrder by remember { mutableStateOf(tasks[currentIndex].items.indices.shuffled()) }
    var message by remember { mutableStateOf("Перетащи элементы (пока просто нажимай по порядку).") }
    var userOrder by remember { mutableStateOf(listOf<Int>()) }

    fun resetTask() {
        val task = tasks[currentIndex]
        shuffledOrder = task.items.indices.shuffled()
        userOrder = emptyList()
        message = "Нажимай пункты в логичном порядке."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(title = "Логические задания", onBack = onBack)

        Text(
            text = "Задание ${currentIndex + 1} из ${tasks.size}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(8.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        val currentTask = tasks[currentIndex]

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shuffledOrder.size) { pos ->
                val realIndex = shuffledOrder[pos]
                val text = currentTask.items[realIndex]
                val isChosen = userOrder.contains(realIndex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isChosen) {
                            val newOrder = userOrder + realIndex
                            userOrder = newOrder

                            if (newOrder.size == currentTask.items.size) {
                                val correct = newOrder == currentTask.correctOrder
                                message = if (correct) {
                                    "Верно! Можно перейти к следующему заданию."
                                } else {
                                    "Есть ошибки. Попробуй ещё раз."
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChosen) Color(0xFFC8E6C9) else Color(0xFFE3F2FD)
                    )
                ) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .padding(16.dp),
                        fontSize = 18.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { resetTask() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сбросить")
            }

            Button(
                onClick = {
                    if (currentIndex < tasks.lastIndex) {
                        currentIndex++
                    } else {
                        currentIndex = 0
                    }
                    resetTask()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Следующее")
            }
        }
    }
}

