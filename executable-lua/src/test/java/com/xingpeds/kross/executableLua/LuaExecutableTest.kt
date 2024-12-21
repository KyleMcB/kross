package com.xingpeds.kross.executableLua

import com.xingpeds.kross.entities.Chan
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asOutputStream
import com.xingpeds.kross.entities.connectTo
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.toLua
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class LuaExecutableTest {

    private val cwd: File = File(".").absoluteFile

    val helloWorldProgram = "print('Hello, World!')" // Lua program as a string

    @Test
    fun manualTest() = runTest {
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val luaFunc = luaEngine.global.load(helloWorldProgram)
        luaEngine.registerFunction("hi".toLua(), luaFunc)
        subject("hi", emptyList(), pipes = Pipes(), env = emptyMap(), cwd = cwd)()
    }

    @Test
    fun outputStream() = runTest {
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val helloWorldProgram = "print('Hello, World!')" // Lua program as a string
        val luaFunc = luaEngine.global.load(helloWorldProgram)
        luaEngine.registerFunction("hi".toLua(), luaFunc)
        val pipe = Chan()
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {

                pipe.connectTo(output.asOutputStream())
            }
            launch {
                subject("hi", emptyList(), pipes = Pipes(programOutput = pipe), env = emptyMap(), cwd = cwd)()
            }
        }.join()
        assertEquals("Hello, World!", output.toString().trim())
    }

    // TODO bug io.read("*a") does not attempt to read from the pipe at all
    val catProgram = """
local input = io.read("*l")
print(input) -- After io.read""".trimIndent()

    //TODO fix test
    @Ignore
    @Test
    fun two() = runTest {
        val output = StringBuilder()
        val input = "hello there"
        val subject = LuaExecutable()
        val luaEngine = LuaEngine
        val luaFunc = luaEngine.global.load(catProgram)
        luaEngine.registerFunction("cat".toLua(), luaFunc)
        val pipes = Pipes(
            programOutput = Chan(),
            programInput = Chan()
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
                subject.invoke("cat", emptyList(), pipes = pipes, env = emptyMap(), cwd = cwd)()
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
