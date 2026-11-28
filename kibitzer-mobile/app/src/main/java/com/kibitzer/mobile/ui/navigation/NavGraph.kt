/*
 * This file is part of kibitzer
 *
 * Copyright (C) 2026 Chris Warren-Smith. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kibitzer.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.ui.screens.AboutScreen
import com.kibitzer.mobile.ui.screens.AiLobbyScreen
import com.kibitzer.mobile.ui.screens.ChatScreen
import com.kibitzer.mobile.ui.screens.ConnectionHelpScreen
import com.kibitzer.mobile.ui.screens.GameOverScreen
import com.kibitzer.mobile.ui.screens.GameRulesScreen
import com.kibitzer.mobile.ui.screens.GameScreen
import com.kibitzer.mobile.ui.screens.HelpScreen
import com.kibitzer.mobile.ui.screens.HomeScreen
import com.kibitzer.mobile.ui.screens.LobbyScreen
import com.kibitzer.mobile.ui.screens.SettingsScreen

object Routes {
  const val HOME = "home"
  const val LOBBY = "lobby"
  const val AI_LOBBY = "ai_lobby"
  const val GAME = "game"
  const val GAME_OVER = "game_over"
  const val HELP = "help"
  const val CONNECTION_HELP = "connection_help"
  const val GAME_RULES = "game_rules"
  const val ABOUT = "about"
  const val CHAT = "chat"
  const val SETTINGS = "settings"
}

@Composable
fun NavGraph(viewModel: GameViewModel) {
  val navController = rememberNavController()
  val state by viewModel.state.collectAsState()

  NavHost(
    navController = navController,
    startDestination = Routes.HOME,
  ) {
    composable(Routes.HOME) {
      HomeScreen(
        onPlayWithFriends = { navController.navigate(Routes.LOBBY) },
        onPlayWithAi = { navController.navigate(Routes.AI_LOBBY) },
        onHelp = { navController.navigate(Routes.HELP) },
        onSettings = { navController.navigate(Routes.SETTINGS) },
      )
    }
    composable(Routes.AI_LOBBY) {
      AiLobbyScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onGameStart = { navController.navigate(Routes.GAME) },
      )
    }
    composable(Routes.LOBBY) {
      LobbyScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onGameStart = { navController.navigate(Routes.GAME) },
      )
    }
    composable(Routes.GAME) {
      GameScreen(
        viewModel = viewModel,
        onGameOver = { navController.navigate(Routes.GAME_OVER) },
        onHelp = { navController.navigate(Routes.HELP) },
        onChat = { navController.navigate(Routes.CHAT) },
      )
    }
    composable(Routes.GAME_OVER) {
      GameOverScreen(
        viewModel = viewModel,
        onPlayAgain = { navController.navigate(Routes.LOBBY) },
        onHome = { navController.popBackStack(Routes.HOME, inclusive = true) },
      )
    }
    composable(Routes.HELP) {
      HelpScreen(
        onBack = { navController.popBackStack() },
        onConnectionHelp = { navController.navigate(Routes.CONNECTION_HELP) },
        onGameRules = { navController.navigate(Routes.GAME_RULES) },
        onAbout = { navController.navigate(Routes.ABOUT) },
      )
    }
    composable(Routes.CONNECTION_HELP) {
      ConnectionHelpScreen(
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.GAME_RULES) {
      GameRulesScreen(
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.ABOUT) {
      AboutScreen(
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.CHAT) {
      ChatScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
      )
    }
    composable(Routes.SETTINGS) {
      SettingsScreen(
        onBack = { navController.popBackStack() },
      )
    }
  }
}
