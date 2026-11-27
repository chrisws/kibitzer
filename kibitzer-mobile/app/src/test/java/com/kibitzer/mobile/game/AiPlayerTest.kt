package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlayerTest {
  private val ai = AiPlayer(seed = 42L)

  @Test
  fun `decide returns skip for unknown session`() {
    val controller = Controller()
    val cmd = ai.decide(controller, 999)
    assertEquals("skip:", cmd)
  }

  @Test
  fun `warlords returns putd or picu command`() {
    val controller = Controller()
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    val cmd = ai.decide(controller, id)
    assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
  }

  @Test
  fun `free returns putd or picu command`() {
    val controller = Controller()
    controller.setGame(Game.FREE)
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    val cmd = ai.decide(controller, id)
    assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
  }

  @Test
  fun `crazy eights returns putd or picu command`() {
    val controller = Controller()
    controller.setGame(Game.CRAZY_EIGHTS)
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    val cmd = ai.decide(controller, id)
    assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
  }

  @Test
  fun `cheat returns bid in bidding phase`() {
    val controller = Controller()
    controller.setGame(Game.CHEAT)
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    val cmd = ai.decide(controller, id)
    assertTrue(cmd.startsWith("bid:"))
  }

  @Test
  fun `old maid returns picu command`() {
    val controller = Controller()
    controller.setGame(Game.OLD_MAID)
    val id = controller.createSession()
    controller.handle("join:0", id)
    controller.handle("shuf:", id)
    controller.handle("deal:", id)

    val cmd = ai.decide(controller, id)
    assertTrue(cmd.startsWith("picu:"))
  }

  @Test
  fun `go fish returns ask command`() {
    val controller = Controller()
    controller.setGame(Game.GO_FISH)
    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    val cmd = ai.decide(controller, id1)
    assertTrue(cmd.startsWith("ask:"))
  }

  @Test
  fun `seeded AI is deterministic`() {
    val controller1 = Controller()
    val id1 = controller1.createSession()
    controller1.handle("join:0", id1)
    controller1.handle("shuf:", id1)
    controller1.handle("deal:", id1)

    val controller2 = Controller()
    val id2 = controller2.createSession()
    controller2.handle("join:0", id2)
    controller2.handle("shuf:", id2)
    controller2.handle("deal:", id2)

    val ai1 = AiPlayer(seed = 123L)
    val ai2 = AiPlayer(seed = 123L)
    val cmd1 = ai1.decide(controller1, id1)
    val cmd2 = ai2.decide(controller2, id2)
    assertEquals(cmd1, cmd2)
  }

  @Test
  fun `warlords AI putd hand size matches discard size`() {
    val controller = Controller()
    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    val ai = AiPlayer(seed = 42L)

    // Play several rounds to build up the discard pile
    repeat(8) {
      val turn = controller.rooms[0].turn
      val cmd = ai.decide(controller, turn)
      val msg = controller.handle(cmd, turn)
      if (msg.id == "gameover") return
    }

    // Verify: if AI decides to putd, hand size must match deck.lastSize
    val turn = controller.rooms[0].turn
    val deck = controller.rooms[0].deck
    val cmd = ai.decide(controller, turn)

    if (cmd.startsWith("putd:")) {
      val handJson = cmd.removePrefix("putd:")
      val hand = Hand.fromJson(handJson)
      val expectedSize = if (deck.discardSize == 0) 1 else deck.lastSize
      assertEquals(
        "AI must play $expectedSize cards but played ${hand.size}",
        expectedSize,
        hand.size,
      )
    }
  }

  @Test
  fun `warlords AI does not loop on invalid play`() {
    val controller = Controller()
    val id1 = controller.createSession()
    val id2 = controller.createSession()
    controller.handle("join:0", id1)
    controller.handle("join:1", id2)
    controller.handle("shuf:", id1)
    controller.handle("deal:", id1)

    val ai = AiPlayer(seed = 42L)

    // Simulate aiTurn logic: decide, handle, check for rejection
    var lastCommand = ""
    var loops = 0
    repeat(10) {
      val turn = controller.rooms[0].turn
      val cmd = ai.decide(controller, turn)
      if (cmd == lastCommand && cmd.startsWith("putd:")) {
        loops++
      }
      lastCommand = cmd
      val msg = controller.handle(cmd, turn)
      if (msg.id == "gameover") return
      // If rejected, the AI should have fallen back to picu
      if (msg.id == "message" && cmd.startsWith("putd:")) {
        // This means the AI produced an invalid play — bug!
        org.junit.Assert.fail("AI produced invalid putd: $cmd (msg: ${msg.data})")
      }
    }
    assertTrue("AI looped on same command $loops times", loops < 3)
  }
}
