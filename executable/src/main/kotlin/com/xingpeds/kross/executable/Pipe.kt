package com.xingpeds.kross.executable

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

private fun log(any: Any) = kotlin.io.println(any)
interface IPipe : Closeable {
    fun luaBinReader(): LuaBinInput
    fun connectTo(input: InputStream)
    fun connectTo(output: OutputStream)
    fun luaWriter(): LuaWriter

    @OptIn(DelicateCoroutinesApi::class)
    fun inputStream(): InputStream
    fun outputStream(): OutputStream
}

class Pipe(
    private val channel: Channel<Int> = Channel(16, onUndeliveredElement = { println("Undelivered element: $it") }),
) : IPipe, Closeable {
    init {
        log("${this@Pipe} created")
    }

    private var _inputStream: InputStream? = null
    private var _outputStream: OutputStream? = null
    private var _luaWriter: LuaWriter? = null
    private var _luaBinInput: LuaBinInput? = null
    override fun luaBinReader(): LuaBinInput {

        return if (this._luaBinInput != null) {
            this._luaBinInput!!
        } else {
            val luaInput = object : LuaBinInput() {

                init {
                    log("${this@Pipe} luabininput created")
                }

                override fun read(): Int = runBlocking {
                    log("${this@Pipe} luabininput read")
                    try {
                        val value = channel.receive()
                        log("${this@Pipe} lua read $value")
                        value
                    } catch (e: ClosedReceiveChannelException) {
                        -1
                    }
                }

                override fun available(): Int {
                    val available = if (channel.isClosedForReceive) 0 else 1
                    return available
                }
            }
            this._luaBinInput = luaInput
            luaInput
        }
    }


    override fun connectTo(input: InputStream) = runBlocking {
        input.use {
            while (true) {
                val byte = it.read()
                log("${this@Pipe} read $byte")
                if (byte == -1) break
                channel.send(byte)
            }
        }
    }.also {
        channel.close()
    }

    override fun connectTo(output: OutputStream) = runBlocking {
        output.use {
            for (byte in channel) {
                log("${this@Pipe} writing $byte")
                output.write(byte)
            }
        }
    }

    override fun luaWriter(): LuaWriter {
        return if (this._luaWriter != null) {
            this._luaWriter!!
        } else {
            val writer = object : LuaWriter() {
                init {
                    log("${this@Pipe} luawriter created")
                }

                override fun print(v: String) = runBlocking {
                    for (byte in v.encodeToByteArray()) {
                        log("${this@Pipe} lua writing $byte")
                        channel.send(byte.toInt())
                    }
                }

                override fun write(value: Int) {
                    runBlocking {
                        log("${this@Pipe} lua writing $value")
                        channel.send(value)
                    }
                }

                override fun close() {
                    log("${this@Pipe} closing writer")
                    channel.close()
                }
            }
            this._luaWriter = writer
            writer
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun inputStream(): InputStream {
        return if (_inputStream != null) {
            this._inputStream!!
        } else {

            val input = object : InputStream() {
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
                    val available = if (channel.isClosedForReceive) {
                        0
                    } else {
                        1
                    }
                    return available
                }
            }
            this._inputStream = input
            input
        }
    }

    override fun outputStream(): OutputStream {
        return if (this._outputStream != null) {
            this._outputStream!!
        } else {
            val output = object : OutputStream() {
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
            this._outputStream = output
            output
        }
    }

    override fun close() {
        log("closing ${this@Pipe}")
        channel.close()
        _inputStream?.close()
        _outputStream?.close()
        _luaWriter?.close()
        _luaBinInput?.close()
    }
}