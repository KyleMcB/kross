package com.xingpeds.kross

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun utf8DecoderFlow(byteFlow: Flow<Int>): Flow<Char> = flow {
    // A small buffer or state machine to decode UTF-8 bytes.
    // For demonstration, let's implement a simplistic approach:

    var buffer = mutableListOf<Byte>()

    suspend fun emitIfComplete() {
        if (buffer.isEmpty()) return
        val bytes = buffer.toByteArray()
        try {
            val str = bytes.toString(Charsets.UTF_8)
            // Typically str will have length 1, but could be multiple chars if combining, etc.
            for (ch in str) {
                emit(ch)
            }
        } catch (e: Exception) {
            // If decoding fails, you might want to handle it differently
            // For now, we just emit replacement characters.
            emit('\uFFFD')
        } finally {
            buffer.clear()
        }
    }

    byteFlow.collect { b ->
        when {
            // For ASCII range, let's short-circuit:
            b < 0x80 -> {
                // If we have some partially-decoded multi-byte sequence, flush it
                emitIfComplete()
                emit(b.toChar())
            }

            else -> {
                // Accumulate in buffer, attempt to decode once we suspect we have enough bytes
                buffer.add(b.toByte())
                // A real approach would check the leading bits to see how many bytes are needed
                // For demonstration, let's just guess we can decode each byte set individually
                // or wait for a short "incomplete" scenario.
                if (buffer.size >= 4) {
                    emitIfComplete()
                }
            }
        }
    }
    // Flush anything left
    emitIfComplete()
}
