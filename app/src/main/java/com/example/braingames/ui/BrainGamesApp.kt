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
import androidx.compose.ui.graphics.Color
import com.example.braingames.feature.differences.DifferencesGameScreen
import com.example.braingames.feature.puzzle.PuzzleGameScreen
import com.example.braingames.feature.sequence.SequenceGameScreen
import com.example.braingames.feature.welcome.WelcomeScreen
import com.example.braingames.ui.menu.MainMenuScreen
import com.example.braingames.ui.navigation.GameScreen
import com.example.braingames.ui.theme.AppTypography

@Composable
fun BrainGamesApp() {
    MaterialTheme(
        typography = AppTypography()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White  // ← Меняем с чёрного на белый
        ) {
            var currentScreen by remember { mutableStateOf<GameScreen>(GameScreen.Welcome) }

            when (currentScreen) {
                GameScreen.Welcome -> WelcomeScreen(
                    onStart = { currentScreen = GameScreen.Menu }
                )
                GameScreen.Menu -> MainMenuScreen(
                    onOpenPuzzle = { currentScreen = GameScreen.Puzzle },
                    onOpenDifferences = { currentScreen = GameScreen.Differences },
                    onOpenSequence = { currentScreen = GameScreen.Sequence }
                )
                GameScreen.Puzzle -> PuzzleGameScreen(onBack = { currentScreen = GameScreen.Menu })
                GameScreen.Differences -> DifferencesGameScreen(onBack = { currentScreen = GameScreen.Menu })
                GameScreen.Sequence -> SequenceGameScreen(onBack = { currentScreen = GameScreen.Menu })
            }
        }
    }
}