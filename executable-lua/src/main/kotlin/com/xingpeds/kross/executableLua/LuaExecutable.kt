package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipes
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
            val function = lua["func"][name].checkfunction() ?: throw Exception("could not find lua function $name")
            return {
// I have something thinking to do about how to invoke the lua function
                val funcReturn: Varargs = function.call()
//                    Vfunction.invoke(
//                    LuaValue.listOf(
//                        args.map {
//                            LuaString.valueOf(it)
//                        }.toTypedArray()
//                    )
//                )
                funcReturn.toint(1)
            }
        } else throw Exception("could not find lua function $name")


    }
}