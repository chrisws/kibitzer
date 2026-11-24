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
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        val cmd = ai.decide(controller, id)
        assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
    }

    @Test
    fun `free returns putd or picu command`() {
        val controller = Controller()
        controller.setGame(Game.FREE)
        val id = controller.createSession()
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        val cmd = ai.decide(controller, id)
        assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
    }

    @Test
    fun `crazy eights returns putd or picu command`() {
        val controller = Controller()
        controller.setGame(Game.CRAZY_EIGHTS)
        val id = controller.createSession()
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        val cmd = ai.decide(controller, id)
        assertTrue(cmd.startsWith("putd:") || cmd.startsWith("picu:"))
    }

    @Test
    fun `cheat returns bid in bidding phase`() {
        val controller = Controller()
        controller.setGame(Game.CHEAT)
        val id = controller.createSession()
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        val cmd = ai.decide(controller, id)
        assertTrue(cmd.startsWith("bid:"))
    }

    @Test
    fun `old maid returns picu command`() {
        val controller = Controller()
        controller.setGame(Game.OLD_MAID)
        val id = controller.createSession()
        controller.handle("join:0", id.toString())
        controller.handle("shuf:", id.toString())
        controller.handle("deal:", id.toString())

        val cmd = ai.decide(controller, id)
        assertTrue(cmd.startsWith("picu:"))
    }

    @Test
    fun `go fish returns ask command`() {
        val controller = Controller()
        controller.setGame(Game.GO_FISH)
        val id1 = controller.createSession()
        val id2 = controller.createSession()
        controller.handle("join:0", id1.toString())
        controller.handle("join:1", id2.toString())
        controller.handle("shuf:", id1.toString())
        controller.handle("deal:", id1.toString())

        val cmd = ai.decide(controller, id1)
        assertTrue(cmd.startsWith("ask:"))
    }

    @Test
    fun `seeded AI is deterministic`() {
        val controller1 = Controller()
        val id1 = controller1.createSession()
        controller1.handle("join:0", id1.toString())
        controller1.handle("shuf:", id1.toString())
        controller1.handle("deal:", id1.toString())

        val controller2 = Controller()
        val id2 = controller2.createSession()
        controller2.handle("join:0", id2.toString())
        controller2.handle("shuf:", id2.toString())
        controller2.handle("deal:", id2.toString())

        val ai1 = AiPlayer(seed = 123L)
        val ai2 = AiPlayer(seed = 123L)
        val cmd1 = ai1.decide(controller1, id1)
        val cmd2 = ai2.decide(controller2, id2)
        assertEquals(cmd1, cmd2)
    }
}