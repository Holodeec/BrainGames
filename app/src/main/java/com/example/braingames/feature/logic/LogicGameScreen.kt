package com.example.braingames.feature.logic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.ui.components.ScreenTitle

// domain
data class LogicTask(
    val items: List<String>,
    val correctOrder: List<Int>
)

private fun provideLogicTasks(): List<LogicTask> = listOf(
    LogicTask(
        items = listOf("Утро", "Ночь", "День", "Вечер"),
        correctOrder = listOf(1, 3, 4, 2)
    ),
    LogicTask(
        items = listOf("Семя", "Цветок", "Росток", "Почка"),
        correctOrder = listOf(1, 3, 4, 2)
    )
)

@Composable
fun LogicGameScreen(onBack: () -> Unit) {
    val tasks = remember { provideLogicTasks() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var shuffledOrder by remember { mutableStateOf(tasks[currentIndex].items.indices.shuffled()) }
    var message by remember {
        mutableStateOf(
            "Нажимай пункты в логичном порядке."
        )
    }
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
                Text("Следующее")
            }
        }
    }
}

