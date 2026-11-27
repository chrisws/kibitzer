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

enum class PlayerState { LURK, JOINED, DEALT }

class Player(
  val sessionId: Int,
) {
  val hand = Hand()
  var nickname: String = "#$sessionId"
    set(value) {
      field = value.replace("\"", "")
    }
  var slot: Int = -1
  var points: Int = 0
  var room: Int = 0
  var state: PlayerState = PlayerState.LURK
  var isAi: Boolean = false

  fun name(): String = "($nickname)"

  fun toJson(): String {
    val active = state != PlayerState.LURK && slot != -1
    return """{"sessionId":$sessionId,"active":$active,"nic":"$nickname","ai":$isAi}"""
  }
}
