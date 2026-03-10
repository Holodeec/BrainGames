package com.example.braingames.feature.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.braingames.R
import com.example.braingames.ui.components.FireworksAnimation
import com.example.braingames.ui.components.ScreenTitle
import com.example.braingames.ui.theme.Orange
import kotlin.math.min

private const val GRID_SIZE = 3
private data class PuzzleLevel(val drawableResId: Int)
private data class PuzzleTile(val index: Int, val image: androidx.compose.ui.graphics.ImageBitmap)

private fun loadPuzzleLevels(): List<PuzzleLevel> {
    return R.drawable::class.java.fields
        .filter { it.name.startsWith("puzzle_") }
        .sortedBy { it.name }
        .mapNotNull { field ->
            try { PuzzleLevel(field.getInt(null)) } catch (_: Exception) { null }
        }
}

private fun splitBitmapToTiles(bitmap: Bitmap, gridSize: Int): List<Bitmap> {
    val size = min(bitmap.width, bitmap.height)
    val tileSize = size / gridSize
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

@Composable
fun PuzzleGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val levels = remember { loadPuzzleLevels() }
    val configuration = LocalConfiguration.current
    val tileSize = (configuration.screenWidthDp / GRID_SIZE) - 16

    if (levels.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Нет доступных картинок для пазлов.\nДобавь файлы puzzle_*.png в папку res/drawable.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Text("Выйти в меню")
            }
        }
        return
    }

    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val currentLevel = levels[currentLevelIndex]
    var puzzleImageSize by remember { mutableStateOf(IntSize.Zero) }

    val allTilesForLevel: List<PuzzleTile> = remember(currentLevel) {
        val bm = BitmapFactory.decodeResource(context.resources, currentLevel.drawableResId)
        splitBitmapToTiles(bm, GRID_SIZE)
            .mapIndexed { index, tileBitmap -> PuzzleTile(index = index, image = tileBitmap.asImageBitmap()) }
    }

    var tilesOrder by remember(currentLevel) { mutableStateOf(allTilesForLevel.shuffled()) }
    var selectedPosition by remember(currentLevel) { mutableStateOf<Int?>(null) }

    val isSolved = tilesOrder.map { it.index } == allTilesForLevel.indices.toList()
    val hasNextLevel = currentLevelIndex < levels.lastIndex

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        ScreenTitle(
            title = "Пазл ${currentLevelIndex + 1} из ${levels.size}",
            onBack = onBack
        )

        Text(
            text = "Нажимай по двум кусочкам, чтобы поменять их местами",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { puzzleImageSize = it }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_SIZE),
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tilesOrder.size) { position ->
                    val tile = tilesOrder[position]
                    val isSelected = selectedPosition == position

                    Card(
                        modifier = Modifier
                            .size(tileSize.dp)
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
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.small)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Orange else Color.LightGray,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Image(
                                bitmap = tile.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            FireworksAnimation(
                modifier = Modifier.fillMaxSize(),
                isActive = isSolved,
                containerSize = puzzleImageSize
            )
        }

        if (isSolved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasNextLevel) "Пазл собран!" else "Игра пройдена!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                textAlign = TextAlign.Center,
                color = Orange
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text(if (hasNextLevel) "Следующий пазл" else "Сыграть сначала")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Выйти")
                }
            }
        } else {
            Button(
                onClick = {
                    tilesOrder = allTilesForLevel.shuffled()
                    selectedPosition = null
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Text("Перемешать")
            }
        }
    }
}