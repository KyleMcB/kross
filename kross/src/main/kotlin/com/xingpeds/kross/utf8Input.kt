package com.xingpeds.kross

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

val altTimeoutMs = 20.milliseconds

sealed class KeyEvent {
    data class Character(val text: String) : KeyEvent()
    data class Alt(val text: String) : KeyEvent()
    data class Ctrl(val code: Int) : KeyEvent()
    data object Escape : KeyEvent()
    // etc.
}

fun toKeyEventFlow(input: Flow<Int>): Flow<KeyEvent> {
    return channelFlow {
        val channel = Channel<Int>(capacity = Channel.UNLIMITED)
        coroutineScope {
            // 1) Collect upstream input bytes into a Channel<Int>
            launch {
                input.collect { byte ->
                    channel.send(byte)
                }
                channel.close()
            }

            // 2) Pull bytes out of the channel and interpret them
            launch {
                while (!channel.isClosedForReceive) {
                    val b = channel.receiveCatching().getOrNull() ?: break

                    when {
                        // 1..31 = control codes (except ESC = 27)
                        b in 1..31 && b != 27 -> {
                            send(KeyEvent.Ctrl(b))
                        }

                        // ESC (27) => check if next byte arrives quickly => Alt combination
                        b == 27 -> {
                            val nextByte = withTimeoutOrNull(altTimeoutMs) {
                                channel.receive()
                            }
                            if (nextByte == null) {
                                // No next byte in time => ESC alone
                                send(KeyEvent.Escape)
                            } else {
                                // Next byte arrived => interpret as Alt + <that char>
                                if (nextByte in 32..126) {
                                    // Simple ASCII Alt
                                    send(KeyEvent.Alt(nextByte.toChar().toString()))
                                } else {
                                    // For anything else, you might do a fallback
                                    // e.g., decode as UTF-8 or handle extended codes
                                    // Here, we’ll just treat it as single Alt for demonstration:
                                    send(KeyEvent.Alt(nextByte.toChar().toString()))
                                }
                            }
                        }

                        else -> {
                            // If it's in the ASCII printable range, emit a Character
                            if (b in 32..126) {
                                send(KeyEvent.Character(b.toChar().toString()))
                            } else {
                                // For anything else (e.g. extended ASCII 128..255),
                                // you could do a fallback decode:
                                //   - pass to a UTF-8 decoder
                                //   - or treat as KeyEvent.Character with extended ASCII
                                // Here we'll just treat it as a "Character" for demonstration:
                                send(KeyEvent.Character(b.toChar().toString()))
                            }
                        }
                    }
                }
            }
        }
    }
}
