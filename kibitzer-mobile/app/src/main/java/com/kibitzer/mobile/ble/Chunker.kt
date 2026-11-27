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

class Chunker(
  private val maxChunkSize: Int = GattConstants.MAX_CHUNK_PAYLOAD,
) {
  private val MAGIC = byteArrayOf(0x4B, 0x42)

  fun split(message: String): List<ByteArray> {
    val payload = message.toByteArray(Charsets.UTF_8)
    val totalChunks = maxOf(1, (payload.size + maxChunkSize - 1) / maxChunkSize)
    val chunks = mutableListOf<ByteArray>()

    for (i in 0 until totalChunks) {
      val start = i * maxChunkSize
      val end = minOf(start + maxChunkSize, payload.size)
      val chunkPayload = payload.copyOfRange(start, end)

      val chunk = ByteArray(4 + chunkPayload.size)
      System.arraycopy(MAGIC, 0, chunk, 0, 2)
      chunk[2] = i.toByte()
      chunk[3] = totalChunks.toByte()
      System.arraycopy(chunkPayload, 0, chunk, 4, chunkPayload.size)
      chunks.add(chunk)
    }
    return chunks
  }

  fun reassemble(chunks: List<ByteArray>): String {
    val total = (chunks.first()[3].toInt() and 0xFF)
    require(chunks.size == total) { "Expected $total chunks, got ${chunks.size}" }

    val sorted = chunks.sortedBy { it[2].toInt() and 0xFF }
    val payload = sorted.flatMap { it.copyOfRange(4, it.size).toList() }.toByteArray()
    return String(payload, Charsets.UTF_8)
  }
}
