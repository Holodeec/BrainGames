package com.example.braingames.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.R
import com.example.braingames.ui.theme.Orange

@Composable
fun MainMenuScreen(
    onOpenPuzzle: () -> Unit,
    onOpenDifferences: () -> Unit,
    onOpenSequence: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Brain Games",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 48.sp,
                color = Orange,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Выбери игру для тренировки",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        item {
            GameCard(
                title = "Собери пазл",
                description = "Переставляй кусочки, чтобы собрать картинку",
                imageRes = R.drawable.puzzle_city,//ic_puzzle_preview, // Создадим временную
                backgroundColor = CardLightBlue,
                onPlayClick = onOpenPuzzle
            )
        }

        item {
            GameCard(
                title = "Найди предметы",
                description = "Найди все спрятанные предметы на картинке",
                imageRes = R.drawable.puzzle_beach,//ic_differences_preview, // Создадим временную
                backgroundColor = CardLightBlue,
                onPlayClick = onOpenDifferences
            )
        }

        item {
            GameCard(
                title = "Запомни ряд",
                description = "Запомни последовательность и повтори её",
                imageRes = R.drawable.puzzle_forest, // Создадим временную
                backgroundColor = CardLightBlue,
                onPlayClick = onOpenSequence
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}