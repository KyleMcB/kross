package com.xingpeds.kross.entities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.InputStream
import java.io.OutputStream

private fun log(any: Any) = Unit

fun Chan() = Channel<Int>(16) { num ->
    System.err.println("Error: Channel not sent $num")
}

suspend fun Channel<Int>.connectTo(output: OutputStream, autoClose: Boolean = true) {
    val channel = this
    withContext(Dispatchers.IO) {
        output.use {
            for (byte in this@connectTo) {
                output.write(byte)
            }
        }
        if (autoClose) channel.close().also { log("channel closed after writing") }
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


suspend fun Channel<Int>.connectTo(input: InputStream, autoClose: Boolean = true) {
    val channel = this
    withContext(Dispatchers.IO) {
        input.use {
            while (channel.isClosedForSend.not()) {
                val byte = input.read()
                if (byte == -1) {
                    channel.close()
                    break
                }
                channel.send(byte)
            }
        }
        if (autoClose) channel.close().also { log("channel closed after reading") }
    }
}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}
