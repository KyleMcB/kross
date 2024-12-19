package com.xingpeds.kross.executable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

// TODO clean up printlns
class PipeTest {
    @Test
    fun simpleLuaWriter() = runTest(timeout = 5.seconds) {
        val pipe = Pipe()
        println("Starting test: simpleLuaWriter")
        val luaWriter = pipe.luaWriter()
        println("yay")
    }

    @Test
    fun one() = runTest(timeout = 10.seconds) {
        val pipe = Pipe()
        val output = StringBuilder()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {

                pipe.connectTo("hello".byteInputStream())
                pipe.close()
                println("Finished writing 'hello' to outputStream")
            }
            launch {
                println("Launching coroutine to read from inputStream")
                pipe.connectTo(output.asOutputStream())
            }
        }.join()

        assertEquals("hello", output.toString())
    }


    @Test
    fun connectToInputStream() = runTest(timeout = 10.seconds) {
        println("Starting test: connectToInputStream")

        val pipe = Pipe()
        val output = StringBuilder()

        val externalInput = "This is data from external InputStream"

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Connecting external input stream to the pipe
            launch {
                pipe.connectTo(externalInput.byteInputStream())
                println("Finished transferring data from external InputStream")
                pipe.close()
            }
            // Reading from the pipe's InputStream
            launch {
                pipe.connectTo(output.asOutputStream())
                println("Finished reading data from pipe's InputStream")
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals(externalInput, output.toString())
    }

    @Test
    fun connectInputToOutput() = runTest(timeout = 10.seconds) {
        println("Starting test: connectInputToOutput")

        val pipe = Pipe()
        val externalInput = "This is a round trip"
        val externalOutput = StringBuilder()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Connecting external input stream to the pipe
            launch {
                pipe.connectTo(externalInput.byteInputStream())
                pipe.close()
                println("Finished transferring data from external InputStream to the pipe")
            }
            // Connecting the pipe to an external output stream
            launch {
                pipe.connectTo(externalOutput.asOutputStream())
                println("Finished transferring data from the pipe to external OutputStream")
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals(externalInput, externalOutput.toString())
    }
}

