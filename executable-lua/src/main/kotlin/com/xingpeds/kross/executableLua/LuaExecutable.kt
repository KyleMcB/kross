package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipes
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

class LuaExecutable : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>
    ): ExecutableResult {
        val engine = com.xingpeds.kross.luaScripting.LuaEngine
        if (engine.userFuncExists(name)) {

            val lua = com.xingpeds.kross.luaScripting.LuaEngine.global
            val originalOutput = lua.STDOUT
            val originalInput = lua.STDIN
            val originalErr = lua.STDERR
            val programOutput = pipes.programOutput
            val programInput = pipes.programInput
            val programError = pipes.programError
            if (programOutput != null) {
                lua.STDOUT = programOutput.luaWriter()
            }
            if (programInput != null) {
                lua.STDIN = programInput.luaBinReader()
            }
            if (programError != null) {
                lua.STDERR = programError.luaWriter()
            }
            val function = lua["func"][name].checkfunction() ?: throw Exception("could not find lua function $name")
            return {
// I have something thinking to do about how to invoke the lua function
                val funcReturn: Varargs =
                    function.invoke(
                        LuaValue.listOf(
                            args.map {
                                LuaString.valueOf(it)
                            }.toTypedArray()
                        ),
                        LuaValue.tableOf(namedValues = env.flatMap { (key, value) ->
                            listOf(LuaString.valueOf(key), LuaString.valueOf(value))
                        }.toTypedArray())
                    )
                programOutput?.close()

                lua.STDOUT = originalOutput
                lua.STDIN = originalInput
                lua.STDERR = originalErr
                funcReturn.toint(1)
            }
        } else throw Exception("could not find lua function $name")


    }
}