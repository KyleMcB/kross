package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Pipe
import com.xingpeds.kross.executable.Pipes
import com.xingpeds.kross.executable.asOutputStream
import com.xingpeds.kross.luaScripting.LuaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LuaExecutableTest {

    @Test
    fun one() = runTest {
        LuaEngine.initJob?.join()
        val subject = LuaExecutable()
        subject("apples", listOf("hellergbo"), pipes = Pipes(), env = emptyMap())()
    }

    @Test
    fun two() = runTest {
        val subject = LuaExecutable()
        val output = StringBuilder()
        val input = StringBuilder()

        val pipes = Pipes(
            programOutput = Pipe(),
            programInput = null
        )
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            launch {
                pipes.programInput?.connectTo(input.asOutputStream())
            }
            launch {
                subject.invoke("print", listOf("hello"), pipes = pipes, env = emptyMap())()
            }
        }.join()
        println("output is: $output")
        println("input is: $input")
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
