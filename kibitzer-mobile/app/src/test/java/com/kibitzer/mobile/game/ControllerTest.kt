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
}