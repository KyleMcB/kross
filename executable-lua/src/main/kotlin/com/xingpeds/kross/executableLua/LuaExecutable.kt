package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipes

class LuaExecutable : Executable {
    //    override suspend fun invoke(name: String, args: List<String>, pipes: Pipes): ExecutableResult = coroutineScope {
//        println("Entering invoke method with function name: $name and arguments: $args")
//        val global = LuaEngine.global
//        val programInput = pipes.programInput?.luaBinReader()
//        val programOutput = pipes.programOutput?.luaWriter()
//        val programError = pipes.programError?.luaWriter()
//
//        val originalOutput = global.STDOUT
//        val originalInput = global.STDIN
//        val originalError = global.STDERR
//        println("Captured original global streams (STDOUT, STDIN, STDERR)")
//        programOutput?.let {
//            launch {
//                println("about to redirect STDOUT")
//                global.STDOUT = it
//                println("Redirected STDOUT")
//            }
//        }
//        launch {
//            programInput?.let {
//                global.STDIN = it
//                println("Changed global.STDIN to programInput")
//            }
//        }
//        launch {
//            programError?.let {
//                global.STDERR = it
//                println("Changed global.STDERR to programError")
//            }
//        }
//
//        val userFunction: LuaFunction = global[name].checkfunction() ?: throw Exception("$name not found")
//        println("Found user function: $name in the global scope")
//
//        val luaArgs = LuaValue.listOf(args.map { LuaValue.valueOf(it) }.toTypedArray())
//        println("Prepared Lua arguments: $luaArgs")
//
//        return@coroutineScope {
//            println("Invoking Lua function: $name with args: $luaArgs")
//            val luaval = userFunction.call(luaArgs)
//            println("Lua function returned value: $luaval")
//
//            programOutput?.close()
//            programInput?.close()
//            programError?.close()
//            global.STDOUT = originalOutput
//            global.STDIN = originalInput
//            global.STDERR = originalError
//            println("Restored original global streams (STDOUT, STDIN, STDERR)")
//
//
//            val result = luaval.toint()
//            println("Returning result: $result")
//            result
//        }
//    }
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>
    ): ExecutableResult {
        TODO("Not yet implemented")
    }
}