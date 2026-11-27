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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerTest {
  private val controller = Controller()

  @Test
  fun `createSession returns unique ids`() {
    val id1 = controller.createSession()
    val id2 = controller.createSession()
    assertTrue(id2 > id1)
  }

  @Test
  fun `findSession returns null for unknown`() {
    assertNull(controller.findSession(999))
  }

  @Test
  fun `findSession returns player after create`() {
    val id = controller.createSession()
    assertNotNull(controller.findSession(id))
  }

  @Test
  fun `handle INIT returns init message`() {
    val id = controller.createSession()
    val msg = controller.handle("init:", id)
    assertEquals("init", msg.id)
  }

  @Test
  fun `handle unknown session returns message`() {
    val msg = controller.handle("init:", 999)
    assertEquals("message", msg.id)
  }

  @Test
  fun `handle unknown command returns message`() {
    val id = controller.createSession()
    val msg = controller.handle("xxxx:", id)
    assertEquals("message", msg.id)
  }

  @Test
  fun `handle JOIN assigns slot`() {
    val id = controller.createSession()
    val msg = controller.handle("join:0", id)
    assertEquals("players", msg.id)
    val player = controller.findSession(id)
    assertNotNull(player)
    assertEquals(0, player!!.slot)
  }

  @Test
  fun `handle NICKNAME sets nickname`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.handle("nicn:TestPlayer", id)
    assertEquals("players", msg.id)
    assertEquals("TestPlayer", controller.findSession(id)!!.nickname)
  }

  @Test
  fun `handle NICKNAME rejects duplicates`() {
    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("nicn:Alice", id1)
    val msg = controller.handle("nicn:Alice", id2)
    assertEquals("message", msg.id)
  }

  @Test
  fun `handle GAME sets game`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.handle("game:CRAZY_EIGHTS", id)
    assertEquals("players", msg.id)
    assertEquals(Game.CRAZY_EIGHTS, controller.getCurrentGame())
  }

  @Test
  fun `handle GAME with unknown name`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.handle("game:NOT_A_GAME", id)
    assertEquals("message", msg.id)
  }

  @Test
  fun `handle SHUFFLE returns shuffle message`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.handle("shuf:", id)
    assertEquals("shuffle", msg.id)
  }

  @Test
  fun `handle DEAL deals cards`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    val msg = controller.handle("deal:", id)
    assertEquals("cards", msg.id)
    val player = controller.findSession(id)!!
    assertTrue(player.hand.size > 0)
  }

  @Test
  fun `handle SKIP by non-turn player is a greeting`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.handle("skip:", id)
    assertEquals("message", msg.id)
  }

  @Test
  fun `handle CHAT returns message`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("nicn:Bob", id)
    val msg = controller.handle("chat:hello world", id)
    assertEquals("message", msg.id)
    assertTrue(msg.data.toString().contains("Bob"))
  }

  @Test
  fun `isGameOver is false initially`() {
    assertFalse(controller.isGameOver())
  }

  @Test
  fun `getCurrentTurn is -1 initially`() {
    assertEquals(-1, controller.getCurrentTurn())
  }

  @Test
  fun `setGame changes rules`() {
    controller.setGame(Game.OLD_MAID)
    assertEquals(Game.OLD_MAID, controller.getCurrentGame())
  }

  @Test
  fun `destroySession removes player`() {
    val id = controller.createSession()
    controller.handle("join:0", id)
    val msg = controller.destroySession(id)
    assertNotNull(msg)
    assertEquals("exit", msg!!.id)
    assertNull(controller.findSession(id))
  }

  @Test
  fun `destroySession returns null for unknown`() {
    assertNull(controller.destroySession(999))
  }

  @Test
  fun `pickup from discard produces valid JSON message`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Play a card to create a discard pile
    val hand = ctrl.findSession(id1)!!.hand
    val card = hand.peek()
    hand.remove(card)
    val playHand = Hand().apply { add(card) }
    ctrl.handle("putd:${playHand.toJson()}", id1)

    // Now pickup from discard - message must not contain unescaped quotes
    val msg = ctrl.handle("picu:1", id2)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    // The message field should parse as valid JSON
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    assertNotNull(obj)
    val message = obj.get("message")?.asString ?: ""
    assertTrue(message.contains("Picked up discard"))
    // Ensure no raw brackets-with-quotes in the message
    assertFalse(message.contains("\"["))
  }

  @Test
  fun `warlords pickup takes entire discard not just one card`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Play 1 card (valid on empty discard) to create a discard pile
    val hand = ctrl.findSession(id1)!!.hand
    val card = hand.pop()
    val playHand = Hand().apply { add(card) }
    ctrl.handle("putd:${playHand.toJson()}", id1)

    // Player 2 picks up - should take ALL discard cards (even with count=1)
    val msg = ctrl.handle("picu:1", id2)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""
    assertTrue(message.contains("Picked up discard"))

    // After taking the discard, player 2 should have the card
    val player2 = ctrl.findSession(id2)!!
    assertTrue(player2.hand.has(card))
  }

  @Test
  fun `warlords pickup resets state so next player can play`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Play 1 card to the discard (valid on empty discard)
    val hand = ctrl.findSession(id1)!!.hand
    val card = hand.pop()
    val playHand = Hand().apply { add(card) }
    ctrl.handle("putd:${playHand.toJson()}", id1)

    // Player 2 picks up the discard
    ctrl.handle("picu:1", id2)

    // Now player 1's turn again - discard is empty so any card is valid
    val player1 = ctrl.findSession(id1)!!
    val nextCard = player1.hand.peek()
    val testHand = Hand().apply { add(nextCard) }
    val msg = ctrl.handle("putd:${testHand.toJson()}", id1)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""
    assertTrue(message.contains("played"))
  }

  @Test
  fun `warlords pickup takes discard even when pack has cards`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // With 2 players, 26 cards each = 52 total, pack should be empty after deal
    // But let's verify: play a card first to create a discard
    val hand = ctrl.findSession(id1)!!.hand
    val card = hand.pop()
    val playHand = Hand().apply { add(card) }
    ctrl.handle("putd:${playHand.toJson()}", id1)

    // Even if pack had cards, Warlords should take the discard
    val msg = ctrl.handle("picu:1", id2)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""
    assertTrue(message.contains("Picked up discard"))
  }

  @Test
  fun `warlords pickup with empty discard falls through to pack`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // No cards played yet, discard is empty
    // Player 1 is on turn after deal. With 2 players all 52 cards are dealt,
    // pack is empty, discard is empty → "nothing to pickup"
    val msg = ctrl.handle("picu:1", id1)
    assertEquals("message", msg.id)
    assertTrue(msg.data.toString().contains("nothing to pickup"))
  }

  @Test
  fun `free game pickup does not take entire discard`() {
    val ctrl = Controller()
    ctrl.setGame(Game.FREE)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Player 1 is on turn after deal. Play a card to create a discard.
    val hand = ctrl.findSession(id1)!!.hand
    val card = hand.pop()
    val playHand = Hand().apply { add(card) }
    ctrl.handle("putd:${playHand.toJson()}", id1)

    // Turn is now player 2. Free game: noPlayTakesDiscard() is false.
    // Pack still has cards (handSize=7, so 38 remain), so pickup draws from pack.
    val msg = ctrl.handle("picu:1", id2)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""
    // Should draw from pack, NOT take the entire discard
    assertTrue(message.contains("Picked:"))
    assertFalse(message.contains("Picked up discard"))
  }

  @Test
  fun `warlords consecutive turns work after pickup`() {
    val ctrl = Controller()
    ctrl.setGame(Game.WARLORDS)
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Turn 1: Player 1 plays a card
    val hand1 = ctrl.findSession(id1)!!.hand
    val card1 = hand1.pop()
    ctrl.handle("putd:${Hand().apply { add(card1) }.toJson()}", id1)

    // Turn 2: Player 2 picks up the discard
    ctrl.handle("picu:1", id2)

    // Turn 3: Player 1 plays again (discard is now empty, any card valid)
    val hand1b = ctrl.findSession(id1)!!.hand
    val card2 = hand1b.pop()
    val msg = ctrl.handle("putd:${Hand().apply { add(card2) }.toJson()}", id1)
    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj =
      com.google.gson
        .Gson()
        .fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""
    assertTrue(message.contains("played"))

    // Turn 4: Player 2 plays (discard has 1 card from player 1's second play)
    val hand2 = ctrl.findSession(id2)!!.hand
    val card3 = hand2.pop()
    val msg2 = ctrl.handle("putd:${Hand().apply { add(card3) }.toJson()}", id2)
    // This should either succeed (if rank is valid) or fail gracefully
    // The key is it should NOT say "must play 0 card"
    if (msg2.id == "message") {
      assertFalse(msg2.data.toString().contains("must play 0"))
    }
  }

  @Test
  fun `gameover message uses newlines not html`() {
    val ctrl = Controller()
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("nicn:Alice", id1)
    ctrl.handle("nicn:Bob", id2)
    ctrl.handle("game:WARLORDS", id1)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    // Give Alice only 1 card so she can win by playing it
    val alice = ctrl.findSession(id1)!!
    val bob = ctrl.findSession(id2)!!
    // Move all of Alice's cards to Bob except one
    while (alice.hand.size > 1) {
      bob.hand.add(alice.hand.pop())
    }
    // Set turn to Alice
    val room = ctrl.rooms[0]
    room.turn = id1

    // Alice plays her last card - should trigger gameover
    val card = alice.hand.peek()
    val hand = Hand().apply { add(card) }
    val msg = ctrl.handle("putd:${hand.toJson()}", id1)

    assertEquals("cards", msg.id)
    val data = msg.data as String
    val obj = com.google.gson.Gson().fromJson(data, com.google.gson.JsonObject::class.java)
    val message = obj.get("message")?.asString ?: ""

    // Should contain newline, not <br/>
    assertTrue("Message should use newlines: $message", message.contains("\n"))
    assertFalse("Message should not contain HTML: $message", message.contains("<br/>"))
    // Should identify the winner (name() includes parentheses)
    assertTrue("Message should name winner: $message", message.contains("(Alice) won the round!"))
    // Turn should be -1 (game over)
    assertEquals(-1, obj.get("turn")?.asInt ?: -2)
  }

  @Test
  fun `gameover increments winner points`() {
    val ctrl = Controller()
    val id1 = ctrl.createSession()
    val id2 = ctrl.createSession()
    ctrl.handle("join:0", id1)
    ctrl.handle("join:1", id2)
    ctrl.handle("nicn:Alice", id1)
    ctrl.handle("nicn:Bob", id2)
    ctrl.handle("game:WARLORDS", id1)
    ctrl.handle("shuf:", id1)
    ctrl.handle("deal:", id1)

    val alice = ctrl.findSession(id1)!!
    val bob = ctrl.findSession(id2)!!
    while (alice.hand.size > 1) {
      bob.hand.add(alice.hand.pop())
    }
    val room = ctrl.rooms[0]
    room.turn = id1

    val card = alice.hand.peek()
    val hand = Hand().apply { add(card) }
    ctrl.handle("putd:${hand.toJson()}", id1)

    assertEquals(1, alice.points)
    assertEquals(0, bob.points)
  }
}