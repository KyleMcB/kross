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

                    if (b == 27) {
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
                                // TODO hand wide characters
                                // For anything else, you might do a fallback
                                // e.g., decode as UTF-8 or handle extended codes
                                // Here, we’ll just treat it as single Alt for demonstration:
                                send(KeyEvent.Alt(nextByte.toChar().toString()))
                            }
                        }
                    } else {
                        send(b.toKeyEvent())
                    }
//                    when (b) {
//                        else -> {
//                            // If it's in the ASCII printable range, emit a Character
//                            if (b in 32..126) {
//                                send(KeyEvent.Character(b.toString()))
//                            } else {
//                                // For anything else (e.g. extended ASCII 128..255),
//                                // you could do a fallback decode:
//                                //   - pass to a UTF-8 decoder
//                                //   - or treat as KeyEvent.Character with extended ASCII
//                                // Here we'll just treat it as a "Character" for demonstration:
//                                send(KeyEvent.Character(b.toString()))
//                            }
//                        }
//                    }
                }
            }
        }
    }
}

fun Int.toKeyEvent() = when (this) {


    1 -> KeyEvent.Ctrl('A')
    2 -> KeyEvent.Ctrl('B')
    3 -> KeyEvent.Ctrl('C')
    4 -> KeyEvent.Ctrl('D')
    5 -> KeyEvent.Ctrl('E')
    6 -> KeyEvent.Ctrl('F')
    7 -> KeyEvent.Ctrl('G')
    8 -> KeyEvent.Backspace
    9 -> KeyEvent.Tab
    10 -> KeyEvent.LF
    11 -> KeyEvent.Unknown("11") // I don't think I need to support vertical tab
    12 -> KeyEvent.Unknown("12") // Form Feed is unsupported
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
    // ESC (27) handle elsewhere

    in (32..126) -> KeyEvent.Character(this.toChar().toString())
    127 -> KeyEvent.Backspace

    else -> KeyEvent.Unknown(this.toString())
}