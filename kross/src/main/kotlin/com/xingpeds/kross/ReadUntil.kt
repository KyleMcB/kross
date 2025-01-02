package com.xingpeds.kross

import com.xingpeds.kross.entities.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val counter = MutableStateFlow(0)

/**
 * We need to read input until certain buttons are pressed. Enter for example
 * or a shortcut key. This is because if read is in a tight loop just sending bytes over the channel,
 * we will steal a byte from the child process that is maybe about to be run. We need a strong guarantee
 * that will not call read at any point that an external process is run.
 */
fun CoroutineScope.readUntil(channel: Channel<Int>, read: () -> Int) = launch {
    counter.update { it + 1 }
    if (counter.value > 1) {
        Log.debug("readUntil aborting. already reading")
        counter.update { it - 1 }
        return@launch
    }
    // I need a buffer to put in a character (Int) and a timestamp
    // this buffer needs to be high performance especially for shifting/appending to the end
    val buffer = MutableStateFlow<List<Pair<Int, Instant>>>(emptyList())
    val now = Clock.System::now
    while (true) {
        val result = read()
        if (result == -1) {
            channel.close()
            break
        }
        if (result == -2) continue
        buffer.update { it + (result to now()) }
        buffer.update {
            if (it.size > 10) it.takeLast(10) else it
        }
        //now do I need to stop reading?
        // I need to compare the end of the buffer to a list of terminal characters
        //first lets look for single byte terminals such as the enter button or ctrl+<letter>
        val shouldStop = terminals.any { terminal ->
            if (buffer.value.size < terminal.size) {
                return@any false
            }
            if (terminal.size == 1) {
                val stop = buffer.value.lastOrNull()?.first == terminal.first()
                stop
            } else {
                // now I need to match alt+n sequences
                val bytesToMatch = terminal.size
                val takeLast = buffer.value.takeLast(bytesToMatch).map { it.first }
                val match = takeLast == terminal
                if (match) {
                    val lastIndex = buffer.value.lastIndex
                    val duration: Duration = buffer.value.last().second - buffer.value[lastIndex - 1].second
                    if (duration < 20.milliseconds) {
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
        if (shouldStop) {
            channel.send(result)
            break
        } else {
            channel.send(result)
        }

    }
    counter.update { it - 1 }
}

private val terminals = listOf(
    listOf(1), // -> KeyEvent.Ctrl('A')
    listOf(2), // -> KeyEvent.Ctrl('B')
    listOf(3), // -> KeyEvent.Ctrl('C')
    listOf(4), // -> KeyEvent.Ctrl('D')
    listOf(5), // -> KeyEvent.Ctrl('E')
    listOf(6), // -> KeyEvent.Ctrl('F')
    listOf(7), // -> KeyEvent.Ctrl('G')
    // 8 , // -> KeyEvent.Backspace
    listOf(9), // -> KeyEvent.Tab
    listOf(10), // -> KeyEvent.LF
    //11 , // -> KeyEvent.Unknown("VT(11)") // I wonder if I can get ctrl k to work?
    //12 , // -> KeyEvent.Unknown("FF(12)")
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
    KeyEvent.Alt("a").toBytes(),
    KeyEvent.Alt("A").toBytes(),
    KeyEvent.Alt("b").toBytes(),
    KeyEvent.Alt("B").toBytes(),
    KeyEvent.Alt("c").toBytes(),
    KeyEvent.Alt("C").toBytes(),
    KeyEvent.Alt("d").toBytes(),
    KeyEvent.Alt("D").toBytes(),
    KeyEvent.Alt("e").toBytes(),
    KeyEvent.Alt("E").toBytes(),
    KeyEvent.Alt("f").toBytes(),
    KeyEvent.Alt("F").toBytes(),
    KeyEvent.Alt("g").toBytes(),
    KeyEvent.Alt("G").toBytes(),
    KeyEvent.Alt("h").toBytes(),
    KeyEvent.Alt("H").toBytes(),
    KeyEvent.Alt("i").toBytes(),
    KeyEvent.Alt("I").toBytes(),
    KeyEvent.Alt("j").toBytes(),
    KeyEvent.Alt("J").toBytes(),
    KeyEvent.Alt("k").toBytes(),
    KeyEvent.Alt("K").toBytes(),
    KeyEvent.Alt("l").toBytes(),
    KeyEvent.Alt("L").toBytes(),
    KeyEvent.Alt("m").toBytes(),
    KeyEvent.Alt("M").toBytes(),
    KeyEvent.Alt("n").toBytes(),
    KeyEvent.Alt("N").toBytes(),
    KeyEvent.Alt("o").toBytes(),
    KeyEvent.Alt("O").toBytes(),
    KeyEvent.Alt("p").toBytes(),
    KeyEvent.Alt("P").toBytes(),
    KeyEvent.Alt("q").toBytes(),
    KeyEvent.Alt("Q").toBytes(),
    KeyEvent.Alt("r").toBytes(),
    KeyEvent.Alt("R").toBytes(),
    KeyEvent.Alt("s").toBytes(),
    KeyEvent.Alt("S").toBytes(),
    KeyEvent.Alt("t").toBytes(),
    KeyEvent.Alt("T").toBytes(),
    KeyEvent.Alt("u").toBytes(),
    KeyEvent.Alt("U").toBytes(),
    KeyEvent.Alt("v").toBytes(),
    KeyEvent.Alt("V").toBytes(),
    KeyEvent.Alt("w").toBytes(),
    KeyEvent.Alt("W").toBytes(),
    KeyEvent.Alt("x").toBytes(),
    KeyEvent.Alt("X").toBytes(),
    KeyEvent.Alt("y").toBytes(),
    KeyEvent.Alt("Y").toBytes(),
    KeyEvent.Alt("z").toBytes(),
    KeyEvent.Alt("Z").toBytes()
)
