package com.xingpeds.kross.entities

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.InputStream
import java.io.OutputStream


fun Chan() = Channel<Int>(Channel.UNLIMITED) { num -> Log.error("UNSENT $num") }


class SupervisorChannel(private val channel: Channel<Int> = Channel(Channel.UNLIMITED)) : Channel<Int> by channel {
    override fun close(cause: Throwable?): Boolean = false
    fun superClose() = channel.close()
}

suspend fun Channel<Int>.connectTo(output: OutputStream, name: String? = null) {
    // I want to log information about the output stream
    val channel = this
    output.use {
        if (name != null) name.info("$name channel to output stream start")
        for (byte in this@connectTo) {
            // this won't stop until the channel is closed
            name?.info("$name recieved $byte over channel")
            output.write(byte)
//            if (byte == -1) {
//                break
//            }
        }
        if (name != null) name.info("$name channel to output stream ended")
    }
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


suspend fun Channel<Int>.connectTo(input: InputStream, name: String? = null) {
    val channel = this
    input.use {
        while (channel.isClosedForSend.not()) {
            val byte = input.read()
            name?.let { Log.info("$name sent $byte over channel") }
            if (byte == -1) {
                channel.close()
                break
            }
            channel.send(byte)
        }
        name?.let { Log.info("$name channel to input stream ended") }
    }
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

