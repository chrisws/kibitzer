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
