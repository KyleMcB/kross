package com.xingpeds.kross.luaScripting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    suspend fun executeLua(
        code: String,
        input: InputStream? = null,
        output: OutputStream? = null,
        error: OutputStream? = null
    )

    suspend fun userFuncExists(name: String): Boolean
}

suspend fun Lua.executeFile(
    file: File,
    input: InputStream? = null,
    output: OutputStream? = null,
    error: OutputStream? = null
) {
    require(file.exists()) { "File does not exist: $file" }
    val codeAsText: String = file.readText()
    this.executeLua(codeAsText, input, output, error)
}

fun String.toLua(): LuaValue = LuaValue.valueOf(this)

object LuaEngine : Lua {
    val _userFunctions = MutableStateFlow<Map<String, LuaFunction>>(emptyMap())
    val userTable = LuaValue.tableOf()

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

    val apiTable = LuaValue.tableOf().apply {
        this["register"] = registerFunction
    }            // Create the `api` table
    val krossTable = LuaValue.tableOf().apply {
        this["api"] = apiTable
    }          // Create the `kross` table
    val global = Globals().apply {
        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib())
        load(CoroutineLib())
        LoadState.install(this)
        LuaC.install(this)
        this["kross"] = krossTable                   // Add the `kross` table to Globals
        this["func"] = userTable
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            _userFunctions.collect { userFuncMap ->

                userFuncMap.forEach { (name, func) ->
                    userTable[name] = func
                    global["func"] = userTable
                }
            }
        }
    }

    override suspend fun executeLua(code: String, input: InputStream?, output: OutputStream?, error: OutputStream?) {

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

    override suspend fun userFuncExists(name: String): Boolean {
        val containsKey = _userFunctions.value.containsKey(name)
        return containsKey
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

fun prettyPrintLuaValue(luaValue: LuaValue, indent: String = "") {
    return when {
        luaValue.istable() -> { // If it's a table, iterate through keys/values
            val table = luaValue.checktable()!!
            val tableContents = mutableListOf<String>()
            val nextKey = LuaValue.NIL

            table.keys().forEach { println(it.tojstring()) }
//            table.keys().forEach { key ->
//                val value = table[key]
//                tableContents.add("$indent${key.tojstring()} = ${prettyPrintLuaValue(value, "$indent  ")}")
//            }

            println("{\n${tableContents.joinToString("\n")}\n}")
        }

        luaValue.isstring() -> println("\"${luaValue.tojstring()}\"")
        else -> {
            println("luavalue type not handled in pretty printer")
        }
    }
}

