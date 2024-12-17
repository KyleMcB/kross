package com.xingpeds.kross.executable

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.InputStream
import java.io.OutputStream

class Pipe(
    private val channel: Channel<Int> = Channel(16, onUndeliveredElement = { println("Undelivered element: $it") }),
) {
    fun luaBinReader(): LuaBinInput = object : LuaBinInput() {
        override fun read(): Int = runBlocking {
            try {
                channel.receive()
            } catch (e: ClosedReceiveChannelException) {
                // I'm going to assume this is like a java stream
                -1
            }
        }

    }

    fun luaWriter(): LuaWriter = object : LuaWriter() {
        override fun print(v: String) = runBlocking {
            for (byte in v.encodeToByteArray())
                channel.send(byte.toInt())
        }

        override fun write(value: Int) {
            runBlocking { channel.send(value) }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun inputStream(): InputStream = object : InputStream() {
        override fun read(): Int {
            return runBlocking {
                try {
                    val data = channel.receive()
                    data
                } catch (e: ClosedReceiveChannelException) {
                    -1
                }
            }
        }

        override fun available(): Int {
            return if (channel.isClosedForReceive) {
                0
            } else {
                1
            }
        }
    }

    fun outputStream(): OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            runBlocking {
                channel.send(b)
            }
        }

        override fun close() {
            runBlocking {
                channel.close()
            }
        }
    }
}