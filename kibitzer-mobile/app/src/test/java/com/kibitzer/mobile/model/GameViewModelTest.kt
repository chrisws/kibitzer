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

package com.kibitzer.mobile.model

import com.kibitzer.mobile.game.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

  private lateinit var connection: FakeGameConnection
  private lateinit var viewModel: GameViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    connection = FakeGameConnection()
    viewModel = GameViewModel(connection)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  // --- Malformed JSON resilience (guards against crash #2) ---

  @Test
  fun `handleStateUpdate survives malformed JSON`() {
    connection.stateListener?.invoke("this is not json at all")
    assertEquals(-1, viewModel.state.value.currentTurn)
  }

  @Test
  fun `handleStateUpdate survives truncated JSON`() {
    connection.stateListener?.invoke("""{"hand":["2C","3C""")
    assertEquals(-1, viewModel.state.value.currentTurn)
  }

  @Test
  fun `handleStateUpdate survives empty string`() {
    connection.stateListener?.invoke("")
    assertEquals(-1, viewModel.state.value.currentTurn)
  }

  @Test
  fun `handleStateUpdate survives JSON array instead of object`() {
    connection.stateListener?.invoke("""[1,2,3]""")
    assertEquals(-1, viewModel.state.value.currentTurn)
  }

  @Test
  fun `handleChatUpdate survives malformed JSON`() {
    connection.chatListener?.invoke("garbage{{{")
    assertTrue(viewModel.state.value.chatLog.isEmpty())
  }

  @Test
  fun `handleChatUpdate survives empty string`() {
    connection.chatListener?.invoke("")
    assertTrue(viewModel.state.value.chatLog.isEmpty())
  }

  @Test
  fun `handleHandshake survives malformed JSON`() {
    connection.handshakeListener?.invoke("not json")
    assertEquals(-1, viewModel.state.value.mySessionId)
  }

  @Test
  fun `handleHandshake survives empty string`() {
    connection.handshakeListener?.invoke("")
    assertEquals(-1, viewModel.state.value.mySessionId)
  }

  // --- Valid state update ---

  @Test
  fun `handleStateUpdate parses valid JSON`() {
    connection.stateListener?.invoke(wrapMessage("cards", validStateJson))
    val state = viewModel.state.value
    assertEquals(2, state.myHand.size)
    assertEquals(0, state.currentTurn)
    assertEquals(0, state.mySessionId)
    assertFalse(state.isGameOver)
  }

  @Test
  fun `handleStateUpdate parses game over`() {
    connection.stateListener?.invoke(wrapMessage("cards", gameOverJson))
    val state = viewModel.state.value
    assertTrue(state.isGameOver)
    assertEquals(-1, state.currentTurn)
  }

  // --- AI game setup ---

  @Test
  fun `startAiGame sets up state correctly`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 1, nickname = "Tester")

    val state = viewModel.state.value
    assertEquals(GameMode.AI, state.gameMode)
    assertEquals(Screen.GAME, state.screen)
    assertEquals(ConnectionState.CONNECTED, state.connectionState)
    assertEquals(2, state.players.size)
    assertTrue(state.myHand.isNotEmpty())
    assertTrue(state.chatLog.isNotEmpty())
  }

  @Test
  fun `startAiGame human player is first`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 1, nickname = "Tester")

    val state = viewModel.state.value
    assertTrue(!state.players[0].isAi)
    assertTrue(state.players[1].isAi)
  }

  @Test
  fun `startAiGame with two AI players`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 2, nickname = "Tester")

    val state = viewModel.state.value
    assertEquals(3, state.players.size)
    assertTrue(!state.players[0].isAi)
    assertTrue(state.players[1].isAi)
    assertTrue(state.players[2].isAi)
  }

  // --- stopAiGame cleanup ---

  @Test
  fun `stopAiGame resets state`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 1)
    viewModel.stopAiGame()

    val state = viewModel.state.value
    assertEquals(GameMode.BLE, state.gameMode)
    assertEquals(Screen.HOME, state.screen)
    assertTrue(state.myHand.isEmpty())
    assertTrue(state.discardPile.isEmpty())
    assertTrue(state.players.isEmpty())
    assertTrue(state.chatLog.isEmpty())
    assertEquals(-1, state.currentTurn)
    assertFalse(state.isGameOver)
  }

  // --- putdown in AI mode ---

  @Test
  fun `putdown with empty hand returns false in AI mode`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 1)
    val result = viewModel.putdown(emptyList())
    assertFalse(result)
  }

  // --- Navigation and message ---

  @Test
  fun `navigateTo updates screen`() {
    viewModel.navigateTo(Screen.GAME_RULES)
    assertEquals(Screen.GAME_RULES, viewModel.state.value.screen)
  }

  @Test
  fun `clearMessage clears message`() {
    viewModel.startAiGame(Game.WARLORDS, numAiPlayers = 1)
    viewModel.clearMessage()
    assertEquals("", viewModel.state.value.message)
  }
}

private val validStateJson =
  """{"hand":["2C","3D"],"pile":[],"turn":0,"message":"","faceDown":false,"game":"warlords","sessionId":0}"""

private val gameOverJson =
  """{"hand":[],"pile":[],"turn":-1,"message":"Game over","faceDown":false,"game":"warlords","sessionId":0}"""

private fun wrapMessage(
  id: String,
  data: String,
): String {
  val obj = com.google.gson.JsonObject()
  obj.addProperty("id", id)
  obj.addProperty("data", data)
  return obj.toString()
}

private class FakeGameConnection : GameConnection {
  var stateListener: ((String) -> Unit)? = null
  var chatListener: ((String) -> Unit)? = null
  var handshakeListener: ((String) -> Unit)? = null
  var connectionListener: ((Boolean) -> Unit)? = null
  var scanListener: ((String) -> Unit)? = null

  override fun connect() {}
  override fun disconnect() {}
  override fun sendCommand(command: String) {}
  override fun setOnStateUpdate(listener: (String) -> Unit) { stateListener = listener }
  override fun setOnChatUpdate(listener: (String) -> Unit) { chatListener = listener }
  override fun setOnHandshake(listener: (String) -> Unit) { handshakeListener = listener }
  override fun setOnConnectionState(listener: (Boolean) -> Unit) { connectionListener = listener }
  override fun isConnected() = false
  override fun startScan() {}
  override fun stopScan() {}
  override fun setOnScanResult(listener: (String) -> Unit) { scanListener = listener }
}