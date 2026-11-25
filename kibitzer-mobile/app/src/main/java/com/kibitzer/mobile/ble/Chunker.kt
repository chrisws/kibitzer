package com.kibitzer.mobile.ble

class Chunker(private val maxChunkSize: Int = GattConstants.MAX_CHUNK_PAYLOAD) {

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