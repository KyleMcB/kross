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
        init {
            println("luabinreader created")
        }

        override fun read(): Int = runBlocking {
            println("luaBinReader::read called")
            try {
                println("waiting for lua read")
                val value = channel.receive()
                println("luabinreader received value: $value")
                value
            } catch (e: ClosedReceiveChannelException) {
                println("sending -1 to lua for closing, channel is closed")
                -1
            }
        }

        override fun available(): Int {
            println("luaBinReader::available called")
            val available = if (channel.isClosedForReceive) 0 else 1
            println("available() returned: $available")
            return available
        }
    }

    fun connectTo(input: InputStream) = runBlocking {
        println("connectTo(InputStream) started")
        input.use {
            println("copying InputStream to outputStream")
            it.copyTo(outputStream())
            println("copy completed")
        }
        println("closing channel after InputStream processing")
        channel.close()
        println("channel closed in connectTo(InputStream)")
    }

    fun connectTo(output: OutputStream) = runBlocking {
        println("connectTo(OutputStream) started")
        println("copying inputStream to OutputStream")
        output.use {
            inputStream().copyTo(it)
            println("copy completed")
        }
        println("connectTo(OutputStream) finished")
    }

    fun luaWriter(): LuaWriter = object : LuaWriter() {
        init {
            kotlin.io.println("luawriter created")
        }

        override fun print(v: String) = runBlocking {
            kotlin.io.println("luaWriter::print called with value: $v")
            for (byte in v.encodeToByteArray()) {
                kotlin.io.println("luaWriter::printing byte: $byte")
                channel.send(byte.toInt())
            }
            kotlin.io.println("luaWriter::print completed")
        }

        override fun write(value: Int) {
            runBlocking {
                kotlin.io.println("luaWriter::writing value: $value")
                channel.send(value)
                kotlin.io.println("luaWriter::write completed")
            }
        }

        override fun close() {
            kotlin.io.println("luaWriter::close called, closing channel")
            channel.close()
            kotlin.io.println("luaWriter::close completed")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun inputStream(): InputStream = object : InputStream() {
        override fun read(): Int {
            println("inputStream::read called")
            return runBlocking {
                try {
                    println("inputStream::waiting to receive data from channel")
                    val data = channel.receive()
                    println("inputStream::received data: $data")
                    data
                } catch (e: ClosedReceiveChannelException) {
                    println("inputStream::channel is closed, returning -1")
                    -1
                }
            }
        }

        override fun available(): Int {
            println("inputStream::available called")
            val available = if (channel.isClosedForReceive) {
                println("inputStream::channel is closed, returning 0")
                0
            } else {
                println("inputStream::channel is open, returning 1")
                1
            }
            return available
        }
    }

    fun outputStream(): OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            println("outputStream::write called with value: $b")
            runBlocking {
                println("outputStream::sending value $b to channel")
                channel.send(b)
                println("outputStream::sent value to channel")
            }
        }

        override fun close() {
            println("outputStream::close called, closing channel")
            runBlocking {
                channel.close()
                println("outputStream::closed channel")
            }
        }
    }
}