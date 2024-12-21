package com.xingpeds.kross.executable

import com.xingpeds.kross.entities.Chan
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asOutputStream
import com.xingpeds.kross.entities.connectTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class JavaOSProcessTest {
    private val cwd: File = File(".").absoluteFile

    @Test
    fun one() = runTest(timeout = 5.seconds) {
        val subject = JavaOSProcess()
        val result = subject("echo", listOf("hello"), cwd = cwd, env = emptyMap())()
        assertEquals(0, result)
    }

    @Test
    fun two() = runTest(timeout = 5.seconds) {
        val subject = JavaOSProcess()
        val outputPipe = Chan()
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                println("invoke")
                subject(
                    "echo", listOf("fake input"), cwd = cwd, env = emptyMap(), pipes = Pipes(
                        programOutput = outputPipe
                    )
                )()
                outputPipe.close()
                println("finished invoke")
            }
            launch {
                println("connect to output")
                outputPipe.connectTo(output.asOutputStream())

                println("finished connect to output")
            }
        }.join()
        assertEquals("fake input", output.toString().trim())
    }

    @Test
    fun three() = runTest(timeout = 5.seconds) {
        val subject = JavaOSProcess()
        val result = StringBuilder()
        val outputPipe = Chan()
        val inputPipe = Chan()
        val programInput = "fake input"
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                outputPipe.connectTo(result.asOutputStream())
            }
            launch {
                inputPipe.connectTo(programInput.byteInputStream())
                inputPipe.close()
            }
            launch {
                subject(
                    "cat", listOf(), cwd = cwd, env = emptyMap(), pipes = Pipes(
                        programOutput = outputPipe,
                        programInput = inputPipe
                    )
                )()
                outputPipe.close()
            }
        }.join()
        assertEquals(programInput, result.toString().trim())
    }
}
