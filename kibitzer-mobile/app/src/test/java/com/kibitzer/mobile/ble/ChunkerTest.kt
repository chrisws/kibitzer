package com.kibitzer.mobile.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkerTest {
  private val chunker = Chunker(maxChunkSize = 10)

  @Test
  fun `split single chunk for short message`() {
    val chunks = chunker.split("hi")
    assertEquals(1, chunks.size)
  }

  @Test
  fun `split multiple chunks for long message`() {
    val msg = "a".repeat(25)
    val chunks = chunker.split(msg)
    assertEquals(3, chunks.size)
  }

  @Test
  fun `chunk header has magic bytes`() {
    val chunks = chunker.split("test")
    assertEquals(0x4B.toByte(), chunks[0][0])
    assertEquals(0x42.toByte(), chunks[0][1])
  }

  @Test
  fun `chunk header has correct index and total`() {
    val msg = "a".repeat(25)
    val chunks = chunker.split(msg)
    assertEquals(0.toByte(), chunks[0][2])
    assertEquals(3.toByte(), chunks[0][3])
    assertEquals(1.toByte(), chunks[1][2])
    assertEquals(3.toByte(), chunks[1][3])
    assertEquals(2.toByte(), chunks[2][2])
    assertEquals(3.toByte(), chunks[2][3])
  }

  @Test
  fun `reassemble roundtrip short message`() {
    val msg = "hello world"
    val chunks = chunker.split(msg)
    val result = chunker.reassemble(chunks)
    assertEquals(msg, result)
  }

  @Test
  fun `reassemble roundtrip long message`() {
    val msg = "The quick brown fox jumps over the lazy dog. ".repeat(5)
    val chunks = chunker.split(msg)
    val result = chunker.reassemble(chunks)
    assertEquals(msg, result)
  }

  @Test
  fun `reassemble roundtrip unicode`() {
    val msg = "héllo wörld 日本語"
    val chunks = chunker.split(msg)
    val result = chunker.reassemble(chunks)
    assertEquals(msg, result)
  }

  @Test
  fun `reassemble works with out-of-order chunks`() {
    val msg = "a".repeat(25)
    val chunks = chunker.split(msg)
    val shuffled = listOf(chunks[2], chunks[0], chunks[1])
    val result = chunker.reassemble(shuffled)
    assertEquals(msg, result)
  }

  @Test
  fun `reassemble throws on wrong chunk count`() {
    val msg = "a".repeat(25)
    val chunks = chunker.split(msg)
    try {
      chunker.reassemble(chunks.drop(1))
      assertTrue("should have thrown", false)
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message!!.contains("Expected"))
    }
  }

  @Test
  fun `split empty string produces one empty chunk`() {
    val chunks = chunker.split("")
    assertEquals(1, chunks.size)
    // 4-byte header only
    assertEquals(4, chunks[0].size)
  }

  @Test
  fun `default chunker uses larger size`() {
    val bigChunker = Chunker()
    val chunks = bigChunker.split("hello")
    assertEquals(1, chunks.size)
  }
}
