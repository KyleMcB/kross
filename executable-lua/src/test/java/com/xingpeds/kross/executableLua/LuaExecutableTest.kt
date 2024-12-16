package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
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

    }
}