package com.xingpeds.kross.luaScripting

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import org.luaj.vm2.lib.*
import java.io.*

interface Lua {
    fun executeLua(code: String, input: InputStream? = null, output: OutputStream? = null, error: OutputStream? = null)
    val userFunctions: StateFlow<Map<String, LuaFunction>>
}

fun Lua.executeFile(file: File, input: InputStream? = null, output: OutputStream? = null, error: OutputStream? = null) {
    require(file.exists()) { "File does not exist: $file" }
    val codeAsText: String = file.readText()
    this.executeLua(codeAsText, input, output, error)
}


object LuaObject : Lua {

    val registerFunction = object : TwoArgFunction() {
        override fun call(nameArg: LuaValue, funcArg: LuaValue): LuaValue {
            val name = nameArg.checkjstring() ?: throw Exception("function name not supplied")// Get string from nameArg
            val function = funcArg.checkfunction()
                ?: throw Exception("lua function not supplied") // Ensure funcArg is a LuaFunction
            _userFunctions.update {
                it.toMutableMap().apply { this[name] = function }
            }
            return LuaValue.NIL // maybe I should return the function?
        }

    }

    val global = Globals().apply {
        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib())
        load(CoroutineLib())
        LoadState.install(this)
        LuaC.install(this)
        val krossTable = LuaValue.tableOf()          // Create the `kross` table
        val apiTable = LuaValue.tableOf()            // Create the `api` table
        apiTable["register"] = registerFunction      // Add `registerFunction` to `api` table
        krossTable["api"] = apiTable                 // Add the `api` table to `kross` table
        this["kross"] = krossTable                   // Add the `kross` table to Globals
    }

    override fun executeLua(code: String, input: InputStream?, output: OutputStream?, error: OutputStream?) {

        val originalStdout = global.STDOUT
        val originalStdin = global.STDIN
        val originalStderr = global.STDERR

        try {
            // Override streams if provided
            if (output != null) global.STDOUT = outputAdapter(output)
            if (input != null) global.STDIN = inputAdapter(input)
            if (error != null) global.STDERR = outputAdapter(error)

            global.load(code).call()
        } finally {
            // Restore original streams
            global.STDOUT = originalStdout
            global.STDIN = originalStdin
            global.STDERR = originalStderr
        }
    }

    val _userFunctions = MutableStateFlow<Map<String, LuaFunction>>(emptyMap())
    override val userFunctions: StateFlow<Map<String, LuaFunction>>
        get() = _userFunctions
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
