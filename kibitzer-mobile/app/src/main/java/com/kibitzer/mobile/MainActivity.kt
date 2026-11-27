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

package com.kibitzer.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kibitzer.mobile.model.GameViewModel
import com.kibitzer.mobile.service.GameService
import com.kibitzer.mobile.ui.navigation.NavGraph
import com.kibitzer.mobile.ui.theme.KibitzerTheme

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: GameViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val service = GameService()
    viewModel = GameViewModel(service)

    setContent {
      KibitzerTheme {
        Surface(
          modifier =
            Modifier
              .fillMaxSize()
              .systemBarsPadding(),
          color = MaterialTheme.colorScheme.background,
        ) {
          NavGraph(viewModel)
        }
      }
    }
  }
}
