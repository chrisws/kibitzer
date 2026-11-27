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

package com.kibitzer.mobile.game

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiVsAiTest {
  @Test
  fun `warlords ai vs ai completes several turns`() {
    val controller = Controller()
    val ai1 = AiPlayer(seed = 1L)
    val ai2 = AiPlayer(seed = 2L)

    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    var turns = 0
    var lastMsg: Message? = null
    while (turns < 20 && !controller.isGameOver()) {
      val turn = controller.getCurrentTurn()
      val ai = if (turn == id1) ai1 else ai2
      val cmd = ai.decide(controller, turn)
      lastMsg = controller.handle(cmd, turn)
      assertNotNull(lastMsg)
      turns++
    }
    assertTrue(turns > 0)
  }

  @Test
  fun `free ai vs ai completes several turns`() {
    val controller = Controller()
    controller.setGame(Game.FREE)
    val ai1 = AiPlayer(seed = 10L)
    val ai2 = AiPlayer(seed = 20L)

    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    var turns = 0
    while (turns < 20 && !controller.isGameOver()) {
      val turn = controller.getCurrentTurn()
      val ai = if (turn == id1) ai1 else ai2
      val cmd = ai.decide(controller, turn)
      controller.handle(cmd, turn)
      turns++
    }
    assertTrue(turns > 0)
  }

  @Test
  fun `crazy eights ai vs ai completes several turns`() {
    val controller = Controller()
    controller.setGame(Game.CRAZY_EIGHTS)
    val ai1 = AiPlayer(seed = 30L)
    val ai2 = AiPlayer(seed = 40L)

    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    var turns = 0
    while (turns < 20 && !controller.isGameOver()) {
      val turn = controller.getCurrentTurn()
      val ai = if (turn == id1) ai1 else ai2
      val cmd = ai.decide(controller, turn)
      controller.handle(cmd, turn)
      turns++
    }
    assertTrue(turns > 0)
  }

  @Test
  fun `ai players do not crash on empty hand`() {
    val controller = Controller()
    val ai = AiPlayer(seed = 5L)

    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    // Simulate empty hand
    controller.findSession(id)!!.hand.clear()
    val cmd = ai.decide(controller, id)
    assertNotNull(cmd)
  }
}
