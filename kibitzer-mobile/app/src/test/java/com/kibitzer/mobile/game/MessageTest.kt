package com.kibitzer.mobile.game

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTest {

    @Test
    fun `stores id and data`() {
        val msg = Message("cards", "hello")
        assertEquals("cards", msg.id)
        assertEquals("hello", msg.data)
    }

    @Test
    fun `toJson with string data`() {
        val msg = Message("init", "welcome")
        val json = msg.toJson()
        assertEquals("""{"id":"init","data":welcome}""", json)
    }

    @Test
    fun `toJson with non-string data`() {
        val msg = Message("count", 42)
        val json = msg.toJson()
        assertEquals("""{"id":"count","data":42}""", json)
    }

    @Test
    fun `toString equals toJson`() {
        val msg = Message("test", "data")
        assertEquals(msg.toJson(), msg.toString())
    }

    @Test
    fun `data class equality`() {
        val a = Message("cards", "hello")
        val b = Message("cards", "hello")
        assertEquals(a, b)
    }

    @Test
    fun `data class inequality`() {
        val a = Message("cards", "hello")
        val b = Message("cards", "world")
        org.junit.Assert.assertNotEquals(a, b)
    }
}