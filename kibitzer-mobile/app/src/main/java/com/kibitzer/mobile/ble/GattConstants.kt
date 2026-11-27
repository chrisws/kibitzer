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

package com.kibitzer.mobile.ble

import java.util.UUID

object GattConstants {
  val SERVICE_UUID = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f1b")
  val CHAR_COMMAND = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f2c")
  val CHAR_STATE = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f3d")
  val CHAR_CHAT = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f4e")
  val CHAR_HANDSHAKE = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f5f")
  val CHAR_PLAYERS = UUID.fromString("6f5a3b2e-7c1d-4e8a-9b3f-2d4e6a8c0f6a")

  const val TARGET_MTU = 512
  const val MAX_PLAYERS = 6
  const val MAX_CHUNKS = 255
  const val CHUNK_HEADER_SIZE = 4
  const val MAX_CHUNK_PAYLOAD = 508
  const val HEARTBEAT_INTERVAL_MS = 10_000L
  const val PONG_TIMEOUT_MS = 20_000L
  const val RECONNECT_BASE_MS = 1_000L
  const val RECONNECT_MAX_ATTEMPTS = 5
  const val SESSION_TIMEOUT_MS = 120_000L
}
