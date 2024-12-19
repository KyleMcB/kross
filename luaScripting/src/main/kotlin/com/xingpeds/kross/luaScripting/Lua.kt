package com.xingpeds.kross.luaScripting

import com.xingpeds.kross.state.Builtin
import com.xingpeds.kross.state.BuiltinFun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseIoLib
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

fun adapter(builtin: BuiltinFun): LuaFunction = object : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs = runBlocking {
        val argList = mutableListOf<String>()
        for (i in 1..args.narg()) {
            val arg = args.arg(i)
            argList.add(arg.tojstring())
        }
        LuaValue.varargsOf(arrayOf(LuaValue.valueOf(builtin(argList))))
    }
}

object LuaEngine : Lua {
    val _userFunctions = MutableStateFlow<Map<String, LuaFunction>>(emptyMap())
    val userTable = LuaValue.tableOf()
    val builtinTable = LuaValue.tableOf().apply {
        Builtin.builtinFuns.forEach { (name: String, func: BuiltinFun) ->
            this[name] = adapter(func)
        }
    }

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
        load(JseIoLib())
        load(MathLib())
        load(OsLib())

        LoadState.install(this)
        LuaC.install(this)
        this["kross"] = krossTable                   // Add the `kross` table to Globals
        this["func"] = userTable
        this["builtin"] = builtinTable
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


