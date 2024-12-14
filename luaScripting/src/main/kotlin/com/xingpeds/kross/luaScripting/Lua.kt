package com.xingpeds.kross.luaScripting

import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import org.luaj.vm2.lib.*
import java.io.*
import java.util.concurrent.ArrayBlockingQueue

interface Lua {
    fun executeLua(code: String, input: InputStream? = null, output: OutputStream? = null, error: OutputStream? = null)
}

fun Lua.executeFile(file: File, input: InputStream? = null, output: OutputStream? = null, error: OutputStream? = null) {
    require(file.exists()) { "File does not exist: $file" }
    val codeAsText: String = file.readText()
    this.executeLua(codeAsText, input, output, error)
}

class GlobalsPool(size: Int) {
    private val pool = ArrayBlockingQueue<Globals>(size)

    init {
        repeat(size) {
            pool.add(createGlobals())
        }
    }

    private fun createGlobals(): Globals {
        return Globals().apply {
            load(BaseLib())
            load(PackageLib())
            load(Bit32Lib())
            load(TableLib())
            load(StringLib())
            load(CoroutineLib())
            LoadState.install(this)
            LuaC.install(this)
        }
    }

    fun acquire(): Globals {
        return pool.take()
    }

    fun release(globals: Globals) {
        pool.put(globals)
    }
}

object LuaObject : Lua {

    val globalsPool = GlobalsPool(3) // Adjust pool size as needed

    override fun executeLua(code: String, input: InputStream?, output: OutputStream?, error: OutputStream?) {
        val globals = globalsPool.acquire()

        val originalStdout = globals.STDOUT
        val originalStdin = globals.STDIN
        val originalStderr = globals.STDERR

        try {
            // Override streams if provided
            if (output != null) globals.STDOUT = outputAdapter(output)
            if (input != null) globals.STDIN = inputAdapter(input)
            if (error != null) globals.STDERR = outputAdapter(error)

            globals.load(code).call()
        } finally {
            // Restore original streams
            globals.STDOUT = originalStdout
            globals.STDIN = originalStdin
            globals.STDERR = originalStderr

            globalsPool.release(globals)
        }
    }
}

fun outputAdapter(output: OutputStream): LuaWriter = object : LuaWriter() {
    private val writer = PrintWriter(output, true)

    override fun print(v: String) {
        writer.print(v)
        writer.flush()
    }

    override fun write(value: Int) {
        writer.write(value)
        writer.flush()
    }

}

fun inputAdapter(input: InputStream): LuaBinInput = object : LuaBinInput() {
    private val reader = BufferedReader(InputStreamReader(input))

    override fun read(): Int = reader.read()
}
