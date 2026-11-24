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
        controller.handle("join:0", id1.toString())
        controller.handle("join:1", id2.toString())
        controller.handle("shuf:", id1.toString())
        controller.handle("deal:", id1.toString())

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
        controller.handle("join:0", id1.toString())
        controller.handle("join:1", id2.toString())
        controller.handle("shuf:", id1.toString())
        controller.handle("deal:", id1.toString())

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
        controller.handle("join:0", id1.toString())
        controller.handle("join:1", id2.toString())
        controller.handle("shuf:", id1.toString())
        controller.handle("deal:", id1.toString())

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
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        // Simulate empty hand
        controller.findSession(id)!!.hand.clear()
        val cmd = ai.decide(controller, id)
        assertNotNull(cmd)
    }
}