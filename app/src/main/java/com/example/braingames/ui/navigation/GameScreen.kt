package com.example.braingames.ui.navigation

sealed class GameScreen {
    data object Menu : GameScreen()
    data object Puzzle : GameScreen()
    data object Differences : GameScreen()
    data object Sequence : GameScreen()
    data object Logic : GameScreen()
}