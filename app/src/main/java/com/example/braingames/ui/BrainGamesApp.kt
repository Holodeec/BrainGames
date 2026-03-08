package com.example.braingames.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.braingames.feature.differences.DifferencesGameScreen
import com.example.braingames.feature.puzzle.PuzzleGameScreen
import com.example.braingames.feature.sequence.SequenceGameScreen
import com.example.braingames.ui.menu.MainMenuScreen
import com.example.braingames.ui.navigation.GameScreen

@Composable
fun BrainGamesApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentScreen by remember { mutableStateOf<GameScreen>(GameScreen.Menu) }

            when (currentScreen) {
                is GameScreen.Menu -> MainMenuScreen(
                    onOpenPuzzle = { currentScreen = GameScreen.Puzzle },
                    onOpenDifferences = { currentScreen = GameScreen.Differences },
                    onOpenSequence = { currentScreen = GameScreen.Sequence }
                )

                is GameScreen.Puzzle -> PuzzleGameScreen(onBack = {
                    currentScreen = GameScreen.Menu
                })

                is GameScreen.Differences -> DifferencesGameScreen(onBack = {
                    currentScreen = GameScreen.Menu
                })

                is GameScreen.Sequence -> SequenceGameScreen(onBack = {
                    currentScreen = GameScreen.Menu
                })
            }
        }
    }
}

