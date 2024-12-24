package com.xingpeds.kross

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Takes a flow of Int codes, buffers them in a Channel<Int>,
 * then allows us to 'receive' or 'peek' them for alt/ESC logic.
 */
suspend fun interpretKeyEventsBuffered(
    inputFlow: Flow<Int>,
    altTimeoutMs: Long = 50L
): Flow<KeyEvent> = flow {
    // 1) Buffer inputFlow into a Channel
    val channel = Channel<Int>(capacity = Channel.UNLIMITED)

    // Launch a concurrent job to pump the flow into the channel
    kotlinx.coroutines.GlobalScope.launch {
        inputFlow.collect { channel.send(it) }
        channel.close()
    }

    // 2) Now read from the channel in a loop
    while (!channel.isClosedForReceive) {
        val code = channel.receiveCatching().getOrNull() ?: break
        when {
            // Control range
            code in 1..31 -> {
                emit(KeyEvent.Ctrl(code))
            }

            // ESC
            code == 27 -> {
                // Wait for the next code with a timeout
                val nextCode = withTimeoutOrNull(altTimeoutMs) {
                    channel.receive()
                }
                if (nextCode == null) {
                    emit(KeyEvent.Escape)
                } else {
                    // Next code arrived => interpret as Alt
                    if (nextCode in 1..31) {
                        emit(KeyEvent.Ctrl(nextCode))
                    } else {
                        emit(KeyEvent.Alt(nextCode.toChar()))
                    }
                }
            }

            // Normal character
            else -> {
                emit(KeyEvent.Character(code.toChar()))
            }
        }
    }
}
