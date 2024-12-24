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
    data class Ctrl(val code: Char) : KeyEvent()
    data object Escape : KeyEvent()
    data class Unknown(val code: String) : KeyEvent()
    data object Tab : KeyEvent()
    data object CR : KeyEvent()
    data object LF : KeyEvent()
    data object Backspace : KeyEvent()

    // ARROW KEYS
    data object UpArrow : KeyEvent()
    data object DownArrow : KeyEvent()
    data object LeftArrow : KeyEvent()
    data object RightArrow : KeyEvent()
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

                    if (b == 27) {
                        // We suspect ESC-based sequence (Alt or arrows, etc.)
                        val secondByte = withTimeoutOrNull(altTimeoutMs) {
                            channel.receive()
                        }
                        if (secondByte == null) {
                            // No second byte => plain Escape
                            send(KeyEvent.Escape)
                        } else {
                            // We have some second byte; check if it's an arrow/control sequence or just Alt+char
                            when (secondByte) {
                                91, 79 -> {
                                    // 91 = '['  ; 79 = 'O'
                                    // Likely arrow keys or other ESC [ / ESC O sequences
                                    val thirdByte = withTimeoutOrNull(altTimeoutMs) {
                                        channel.receive()
                                    }
                                    if (thirdByte == null) {
                                        // Timed out => we can't parse it fully
                                        send(KeyEvent.Unknown("ESC $secondByte (no third byte)"))
                                    } else {
                                        // Check for arrow codes
                                        when (thirdByte) {
                                            65 -> send(KeyEvent.UpArrow)    // 'A'
                                            66 -> send(KeyEvent.DownArrow)  // 'B'
                                            67 -> send(KeyEvent.RightArrow) // 'C'
                                            68 -> send(KeyEvent.LeftArrow)  // 'D'
                                            else -> {
                                                // Not a recognized arrow => treat as unknown or custom
                                                send(KeyEvent.Unknown("ESC $secondByte $thirdByte"))
                                            }
                                        }
                                    }
                                }

                                in 32..126 -> {
                                    // Simple ASCII => Alt+char
                                    send(KeyEvent.Alt(secondByte.toChar().toString()))
                                }

                                else -> {
                                    // Fallback for any other code after ESC
                                    send(KeyEvent.Alt(secondByte.toChar().toString()))
                                }
                            }
                        }
                    } else {
                        // Not ESC => interpret single-byte code directly
                        send(b.toKeyEvent())
                    }
                }
            }
        }
    }
}

/**
 * Basic ASCII-based interpretation for single bytes (non-ESC).
 * ESC (27) is handled separately above.
 */
fun Int.toKeyEvent() = when (this) {
    // Control letters: 1..26 => Ctrl('A'..'Z') except for special ones like Tab, etc.
    1 -> KeyEvent.Ctrl('A')
    2 -> KeyEvent.Ctrl('B')
    3 -> KeyEvent.Ctrl('C')
    4 -> KeyEvent.Ctrl('D')
    5 -> KeyEvent.Ctrl('E')
    6 -> KeyEvent.Ctrl('F')
    7 -> KeyEvent.Ctrl('G')
    8 -> KeyEvent.Backspace    // could also be ASCII 127 for backspace
    9 -> KeyEvent.Tab
    10 -> KeyEvent.LF
    11 -> KeyEvent.Unknown("VT(11)")  // not supported
    12 -> KeyEvent.Unknown("FF(12)")  // not supported
    13 -> KeyEvent.CR
    14 -> KeyEvent.Ctrl('N')
    15 -> KeyEvent.Ctrl('O')
    16 -> KeyEvent.Ctrl('P')
    17 -> KeyEvent.Ctrl('Q')
    18 -> KeyEvent.Ctrl('R')
    19 -> KeyEvent.Ctrl('S')
    20 -> KeyEvent.Ctrl('T')
    21 -> KeyEvent.Ctrl('U')
    22 -> KeyEvent.Ctrl('V')
    23 -> KeyEvent.Ctrl('W')
    24 -> KeyEvent.Ctrl('X')
    25 -> KeyEvent.Ctrl('Y')
    26 -> KeyEvent.Ctrl('Z')

    // 27 -> handled in the ESC logic above

    in 32..126 -> KeyEvent.Character(this.toChar().toString())  // ASCII printable
    127 -> KeyEvent.Backspace

    else -> KeyEvent.Unknown(this.toString())
}
