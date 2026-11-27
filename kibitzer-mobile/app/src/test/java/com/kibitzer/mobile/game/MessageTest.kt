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
