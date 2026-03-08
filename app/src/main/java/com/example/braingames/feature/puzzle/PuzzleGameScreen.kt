package com.example.braingames.feature.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.braingames.R
import com.example.braingames.ui.components.ScreenTitle
import kotlin.math.min

// --- Domain / data ---

private const val GRID_SIZE = 3 // 3x3 для 600x600 => тайл 200x200

private data class PuzzleLevel(val drawableResId: Int)

private data class PuzzleTile(
    val index: Int, // правильная позиция тайла
    val image: ImageBitmap
)

/**
 * Загружаем все изображения-пазлы из ресурсов.
 * Достаточно положить картинки 600x600 в res/drawable с префиксом "puzzle_".
 * Примеры имён: puzzle_cat, puzzle_dog, puzzle_farm_1 и т.п.
 */
private fun loadPuzzleLevels(): List<PuzzleLevel> {
    val fields = R.drawable::class.java.fields

    return fields
        .filter { it.name.startsWith("puzzle_") }
        .sortedBy { it.name } // порядок уровней по имени файла
        .mapNotNull { field ->
            try {
                val resId = field.getInt(null)
                PuzzleLevel(resId)
            } catch (_: Exception) {
                null
            }
        }
}

private fun splitBitmapToTiles(bitmap: Bitmap, gridSize: Int): List<Bitmap> {
    val size = min(bitmap.width, bitmap.height)
    val tileSize = size / gridSize

    // Обрезаем до квадрата по центру, если картинка не квадратная
    val startX = (bitmap.width - size) / 2
    val startY = (bitmap.height - size) / 2

    val tiles = mutableListOf<Bitmap>()
    for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
            val x = startX + col * tileSize
            val y = startY + row * tileSize
            tiles.add(Bitmap.createBitmap(bitmap, x, y, tileSize, tileSize))
        }
    }
    return tiles
}

// --- UI ---

@Composable
fun PuzzleGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Уровни определяются динамически по картинкам puzzle_* в drawable
    val levels = remember { loadPuzzleLevels() }

    if (levels.isEmpty()) {
        // Нет ни одной картинки – показываем сообщение и кнопку "Назад"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScreenTitle(title = "Пазл: собери картинку", onBack = onBack)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет доступных картинок для пазлов.\nДобавь файлы puzzle_*.png в папку res/drawable.",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Выйти в меню")
            }
        }
        return
    }

    var currentLevelIndex by remember { mutableIntStateOf(0) }

    // Загружаем bitmap и режем на тайлы один раз на уровень
    val currentLevel = levels[currentLevelIndex]
    val allTilesForLevel: List<PuzzleTile> = remember(currentLevel) {
        val bm = BitmapFactory.decodeResource(context.resources, currentLevel.drawableResId)
        splitBitmapToTiles(bm, GRID_SIZE)
            .mapIndexed { index, tileBitmap ->
                PuzzleTile(index = index, image = tileBitmap.asImageBitmap())
            }
    }

    var tilesOrder by remember(currentLevel) { mutableStateOf(allTilesForLevel.shuffled()) }
    var selectedPosition by remember(currentLevel) { mutableStateOf<Int?>(null) }

    val isSolved = tilesOrder.map { it.index } == allTilesForLevel.indices.toList()
    val hasNextLevel = currentLevelIndex < levels.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenTitle(
            title = "Пазл ${currentLevelIndex + 1} из ${levels.size}",
            onBack = onBack
        )

        Text(
            text = "Нажимай по двум кусочкам, чтобы поменять их местами и собрать картинку.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_SIZE),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tilesOrder.size) { position ->
                val tile = tilesOrder[position]
                val isSelected = selectedPosition == position

                Card(
                    modifier = Modifier
                        .size(110.dp)
                        .clickable(enabled = !isSolved) {
                            if (selectedPosition == null) {
                                selectedPosition = position
                            } else {
                                val first = selectedPosition!!
                                if (first != position) {
                                    val mutable = tilesOrder.toMutableList()
                                    val tmp = mutable[first]
                                    mutable[first] = mutable[position]
                                    mutable[position] = tmp
                                    tilesOrder = mutable
                                }
                                selectedPosition = null
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = tile.image,
                            contentDescription = "Фрагмент пазла",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (isSolved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasNextLevel) "Пазл собран!" else "Игра пройдена!",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedPosition = null
                        if (hasNextLevel) {
                            currentLevelIndex++
                        } else {
                            currentLevelIndex = 0
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (hasNextLevel) "Следующий пазл" else "Сыграть сначала")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Выйти в меню")
                }
            }
        } else {
            Button(
                onClick = {
                    tilesOrder = allTilesForLevel.shuffled()
                    selectedPosition = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Перемешать")
            }
        }
    }
}

