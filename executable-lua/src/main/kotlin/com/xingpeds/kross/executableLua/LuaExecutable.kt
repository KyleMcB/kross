package com.xingpeds.kross.executableLua

import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asLuaBinInput
import com.xingpeds.kross.entities.asLuaWriter
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.luaScripting.UserDisplayError
import com.xingpeds.kross.luaScripting.key
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import java.io.File

class LuaExecutable : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult {
        val lua = com.xingpeds.kross.luaScripting.LuaEngine.getLuaGlobal({ it }, {})
        val originalOutput = lua.STDOUT
        val originalInput = lua.STDIN
        val originalErr = lua.STDERR
        val programOutput = pipes.programOutput
        val programInput = pipes.programInput
        val programError = pipes.programError
        if (programOutput != null) {
            lua.STDOUT = programOutput.asLuaWriter()
        }
        if (programInput != null) {
            lua.STDIN = programInput.asLuaBinInput()
        }
        if (programError != null) {
            lua.STDERR = programError.asLuaWriter()
        }

        val function = lua["userFuncs"].key(name)?.key("callback")?.checkfunction()
            ?: throw UserDisplayError("could not find lua function $name")

        return {
            val luaArgs = LuaValue.listOf(
                args.map { LuaString.valueOf(it) }.toTypedArray()
            )

            val luaEnv = LuaValue.tableOf(
                namedValues = env.flatMap { (key, value) ->
                    listOf(LuaString.valueOf(key), LuaString.valueOf(value))
                }.toTypedArray()
            )

            val luaFileCwd = LuaFileWrapper(cwd)

            // Pack the `luaArgs`, `luaEnv`, and `luaCwd` into a single Lua table
            val luaInputTable = LuaValue.tableOf(
                arrayOf(
                    LuaValue.valueOf("args"),
                    luaArgs,
                    LuaValue.valueOf("env"),
                    luaEnv,
                    LuaValue.valueOf("cwd"),
                    luaFileCwd
                )
            )

            // Pass the packed table to the Lua function
            val funcReturn: Varargs = function.invoke(luaInputTable)

            programOutput?.close()

            lua.STDOUT = originalOutput
            lua.STDIN = originalInput
            lua.STDERR = originalErr

            funcReturn.toint(1)
        }
    }
}
