package com.xingpeds.kross.executable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class JavaOSProcessTest {
    @Test
    fun one() = runTest {
        val subject = JavaOSProcess()
        val result = subject("echo", listOf("hello"))()
        assertEquals(0, result)
    }

    @Test
    fun two() = runTest {
        val subject = JavaOSProcess()
        val outputPipe = Pipe()
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                subject(
                    "echo", listOf("fake input"), pipes = Pipes(
                        programOutput = outputPipe
                    )
                )()
            }
            launch {
                outputPipe.inputStream().use { it.copyTo(output.asOutputStream()) }
            }
        }.join()
        assertEquals("fake input", output.toString().trim())
    }

    @Test
    fun three() = runTest {
        val subject = JavaOSProcess()
        val result = StringBuilder()
        val outputPipe = Pipe()
        val inputPipe = Pipe()
        val programInput = "fake input"
        inputPipe.connectTo(programInput.byteInputStream())
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                outputPipe.connectTo(result.asOutputStream())
            }
            launch {
                subject(
                    "cat", listOf(), pipes = Pipes(
                        programOutput = outputPipe,
                        programInput = inputPipe
                    )
                )()
            }
        }.join()
        assertEquals(programInput, result.toString().trim())
    }
}
