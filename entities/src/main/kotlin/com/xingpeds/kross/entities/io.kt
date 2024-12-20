package com.xingpeds.kross.entities

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.InputStream
import java.io.OutputStream

private fun log(any: Any) = println("IO: $any")

fun Chan() = Channel<Int>(16) { num -> log("UNSENT $num") }

class Pipe(private val channel: Channel<Int> = Channel(16)) : Channel<Int> by channel {

    override fun close(cause: Throwable?): Boolean {
        return channel.close(cause)
    }
}

class SupervisorChannel(private val channel: Channel<Int> = Channel(16)) : Channel<Int> by channel {
    override fun close(cause: Throwable?): Boolean = false
    fun superClose() = channel.close()
}

suspend fun Channel<Int>.connectTo(output: OutputStream, autoClose: Boolean = true) {
    val channel = this
    output.use {
        for (byte in this@connectTo) {
            // this won't stop until the channel is closed
            log("writing $byte")
            output.write(byte)
            if (byte == -1) {
                break
            }
        }
        log("exit write loop")
    }
    if (autoClose) channel.close().also { log("channel closed after writing") }
}

fun Channel<Int>.asLuaBinInput(): LuaBinInput {
    val channel = this
    return object : LuaBinInput() {
        override fun read(): Int {
            return runBlocking {
                channel.receive()
            }
        }
    }
}

fun Channel<Int>.asLuaWriter(): LuaWriter {
    val channel = this
    return object : LuaWriter() {
        override fun print(v: String) {
            runBlocking {
                for (byte in v.encodeToByteArray()) {
                    channel.send(byte.toInt())
                }
            }
        }

        override fun write(value: Int) {
            runBlocking {
                channel.send(value)
            }
        }
    }
}


suspend fun Channel<Int>.connectTo(input: InputStream, autoClose: Boolean = true) {
    val channel = this
    input.use {
        while (channel.isClosedForSend.not()) {
            val byte = input.read()
            if (byte == -1) {
                if (autoClose) channel.close()
                break
            }
            channel.send(byte)
        }
        log("exit read loop")
    }
    if (autoClose) channel.close().also { log("channel closed after reading") }
}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}

data class Pipes(
    val programInput: Channel<Int>? = null,
    val programOutput: Channel<Int>? = null,
    val programError: Channel<Int>? = null,
)

