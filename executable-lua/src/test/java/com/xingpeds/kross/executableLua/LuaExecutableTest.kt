package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.asOutputStream
import com.xingpeds.kross.executable.copyToSuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LuaExecutableTest {

    @Test
    fun one() = runTest {
        val subject = LuaExecutable()
        subject.invoke("echo", listOf("hello")).waitFor()
    }

    @Test
    fun two() = runTest {
        val subject = LuaExecutable()
        val programInput = "fake input"
        val process = subject.invoke("cat", emptyList(), output = Executable.StreamSetting.Pipe)
        process.output?.use { it.write(programInput.encodeToByteArray()) }
        process.waitFor()

    }

    @Test
    fun bla() = runTest {
        val programInput = "fake input"
        println(programInput.encodeToByteArray())
    }

    @Test
    fun three() = runTest {
        val subject = LuaExecutable()
        val programInput = "fake input"
        val output = StringBuilder()
        val process = subject.invoke(
            "cat",
            emptyList(),
            output = Executable.StreamSetting.Pipe,
            input = Executable.StreamSetting.Pipe
        )
        process.output?.use { it.write(programInput.encodeToByteArray()) }
        process.input?.use { it.copyToSuspend(output.asOutputStream()) }

        println("result is: $output")
    }
}
