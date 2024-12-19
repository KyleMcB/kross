package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Pipe
import com.xingpeds.kross.executable.Pipes
import com.xingpeds.kross.executable.asOutputStream
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.toLua
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LuaExecutableTest {

    val helloWorldProgram = "print('Hello, World!')" // Lua program as a string

    @Test
    fun manualTest() = runTest {
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val luaFunc = luaEngine.global.load(helloWorldProgram)
        luaEngine.registerFunction("hi".toLua(), luaFunc)
        subject("hi", emptyList(), pipes = Pipes(), env = emptyMap())()
    }

    @Test
    fun outputStream() = runTest {
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val helloWorldProgram = "print('Hello, World!')" // Lua program as a string
        val luaFunc = luaEngine.global.load(helloWorldProgram)
        luaEngine.registerFunction("hi".toLua(), luaFunc)
        val pipe = Pipe()
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {

                pipe.connectTo(output.asOutputStream())
            }
            launch {
                subject("hi", emptyList(), pipes = Pipes(programOutput = pipe), env = emptyMap())()
            }
        }.join()
        assertEquals("Hello, World!", output.toString().trim())
    }

    // TODO bug io.read("*a") does not attempt to read from the pipe at all
    val catProgram = """
local input = io.read("*l")
print(input) -- After io.read""".trimIndent()

    @Test
    fun two() = runTest {
        val output = StringBuilder()
        val input = "hello there"
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val luaFunc = luaEngine.global.load(catProgram)
        luaEngine.registerFunction("cat".toLua(), luaFunc)
        val pipes = Pipes(
            programOutput = Pipe(),
            programInput = Pipe()
        )
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                println("started output")
                pipes.programOutput?.connectTo(output.asOutputStream())
                println("finished output")
            }
            launch {
                println("started input")
                pipes.programInput?.connectTo(input.byteInputStream())
                pipes.programInput?.close()
                println("finished input")
            }
            launch {
                println("started invoke")
                subject.invoke("cat", emptyList(), pipes = pipes, env = emptyMap())()
                println("finished invoke")
            }
        }.join()
        println("output is: $output")
        println("input is: $input")
        assertEquals(input, output.toString().trim())
    }

//    @Test
//    fun two() = runTest {
//        val subject = LuaExecutable()
//        val programInput = "fake input"
//        val process = subject.invoke("cat", emptyList(), output = Executable.StreamSetting.Pipe)
//        process.output?.use { it.write(programInput.encodeToByteArray()) }
//        process.waitFor()
//
//    }
//
//    @Test
//    fun bla() = runTest {
//        val programInput = "fake input"
//        println(programInput.encodeToByteArray())
//    }
//
//    @Test
//    fun three() = runTest {
//        val subject = LuaExecutable()
//        val programInput = "fake input"
//        val output = StringBuilder()
//        val process = subject.invoke(
//            "cat",
//            emptyList(),
//            output = Executable.StreamSetting.Pipe,
//            input = Executable.StreamSetting.Pipe
//        )
//        process.output?.use { it.write(programInput.encodeToByteArray()) }
//        process.input?.use { it.copyToSuspend(output.asOutputStream()) }
//
//        println("result is: $output")
//    }
}
