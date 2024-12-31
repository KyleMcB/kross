package com.xingpeds.kross

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * We need to read input until certain buttons are pressed. Enter for example
 * or a shortcut key. This is because if read is in a tight loop just sending bytes over the channel,
 * we will steal a byte from the child process that is maybe about to be run. We need a strong guarantee
 * that will not call read at any point that an external process is run.
 */
fun CoroutineScope.readUntil(channel: Channel<Int>, read: () -> Int) = launch {
    // I need a buffer to put in a character (Int) and a timestamp
    // this buffer needs to be high performance especially for shifting/appending to the end
    // Probably just a list though
    val buffer = mutableListOf<Pair<Int, Instant>>()
    val now = Clock.System::now
    while (true) {
        val result = read()
        if (result == -1) {
            channel.close()
            break
        }
        if (result == -2) continue
        buffer.add(result to now())
        channel.send(result)
        //now do I need to stop reading?
        // I need to compare the end of the buffer to a list of terminal characters
        //first lets look for single byte terminals such as the enter button or ctrl+<letter>
        if (terminals.any { terminal ->
                if (terminal.size == 1) {
                    buffer.lastOrNull()?.first == terminal.first()
                } else {
                    // now I need to match alt+n sequences
                    false // TODO replace with actual match to the end of buffer
                }
            }) {
            break
        }

    }
}

private val terminals = listOf(
    listOf(1), // -> KeyEvent.Ctrl('A')
    listOf(2), // -> KeyEvent.Ctrl('B')
    listOf(3), // -> KeyEvent.Ctrl('C')
    listOf(4), // -> KeyEvent.Ctrl('D')
    listOf(5), // -> KeyEvent.Ctrl('E')
    listOf(6), // -> KeyEvent.Ctrl('F')
    listOf(7), // -> KeyEvent.Ctrl('G')
// 8 , // -> KeyEvent.Backspace    // could also be ASCII 127 for backspace
    listOf(9), // -> KeyEvent.Tab
    listOf(10), // -> KeyEvent.LF
//11 , // -> KeyEvent.Unknown("VT(11)")  // not supported
//12 , // -> KeyEvent.Unknown("FF(12)")  // not supported
    listOf(13), // -> KeyEvent.CR
    listOf(14), // -> KeyEvent.Ctrl('N')
    listOf(15), // -> KeyEvent.Ctrl('O')
    listOf(16), // -> KeyEvent.Ctrl('P')
    listOf(17), // -> KeyEvent.Ctrl('Q')
    listOf(18), // -> KeyEvent.Ctrl('R')
    listOf(19), // -> KeyEvent.Ctrl('S')
    listOf(20), // -> KeyEvent.Ctrl('T')
    listOf(21), // -> KeyEvent.Ctrl('U')
    listOf(22), // -> KeyEvent.Ctrl('V')
    listOf(23), // -> KeyEvent.Ctrl('W')
    listOf(24), // -> KeyEvent.Ctrl('X')
    listOf(25), // -> KeyEvent.Ctrl('Y')
    listOf(26), // -> KeyEvent.Ctrl('Z')
)
