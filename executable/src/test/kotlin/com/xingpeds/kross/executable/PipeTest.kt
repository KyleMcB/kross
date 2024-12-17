package com.xingpeds.kross.executable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class PipeTest {
    @Test
    fun one() = runTest(timeout = 10.seconds) {
        println("Starting test: one")
        // regular stream
        val pipe = Pipe()
        val output = StringBuilder()
        val outputStream = pipe.outputStream()
//        outputStream.use {
//            it.write("hello".encodeToByteArray())
//        }
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {


            launch {
                println("Launching coroutine to write to outputStream")
                println("Writing 'hello' to outputStream")
                outputStream.write("hello".encodeToByteArray())
                println("Finished writing 'hello' to outputStream")
                outputStream.close()
                println("Closed outputStream")
            }
            launch {
                println("Launching coroutine to read from inputStream")
                pipe.inputStream().use {
                    println("Reading data from inputStream")
                    it.copyTo(output.asOutputStream())
                    println("Finished reading data from inputStream")
                }
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals("hello", output.toString())
    }
}