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
        val outputStream = pipe.outputStream()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                outputStream.write("hello".encodeToByteArray())
                println("Finished writing 'hello' to outputStream")
                outputStream.close()
            }
            launch {
                println("Launching coroutine to read from inputStream")
                pipe.inputStream().use {
                    println("Reading data from inputStream")
                    it.copyTo(output.asOutputStream())
                }
            }
        }.join()

        assertEquals("hello", output.toString())
    }

    @Test
    fun inputWaitsForOutput() = runTest(timeout = 10.seconds) {

        val pipe = Pipe()
        val output = StringBuilder()

        val inputStream = pipe.inputStream()
        val outputStream = pipe.outputStream()

        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            launch {
                // Writing characters 'h', 'e', 'l', 'l', 'o'
                "hello".forEach { char ->
                    outputStream.write(char.code)
                    kotlinx.coroutines.delay(500) // Simulate slow writing
                }
                outputStream.close()
            }
            launch {
                var byte = inputStream.read()
                while (byte != -1) {
                    output.append(byte.toChar())
                    byte = inputStream.read()
                }
            }
        }.join()

        assertEquals("hello", output.toString())
    }

    @Test
    fun longMessageSynchronization() = runTest(timeout = 10.seconds) {

        val pipe = Pipe()
        val output = StringBuilder()

        val inputStream = pipe.inputStream()
        val outputStream = pipe.outputStream()

        val longMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Vestibulum posuere lacus quis erat scelerisque, non dictum magna tincidunt."

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                outputStream.write(longMessage.encodeToByteArray())
                outputStream.close()
            }
            launch {
                inputStream.use {
                    it.copyTo(output.asOutputStream())
                }
            }
        }.join()
        assertEquals(longMessage, output.toString())
    }

    @Test
    fun interleavedReadingAndWriting() = runTest(timeout = 10.seconds) {

        val pipe = Pipe()
        val output = StringBuilder()

        val inputStream = pipe.inputStream()
        val outputStream = pipe.outputStream()

        val writeData = listOf("hello", "world")

        CoroutineScope(Dispatchers.Default).launch {
            launch {
                for (data in writeData) {
                    outputStream.write(data.encodeToByteArray())
                    kotlinx.coroutines.delay(500) // Simulate real-time writing
                }
                outputStream.close()
                println("Closed outputStream")
            }

            launch {
                var byte = inputStream.read()
                while (byte != -1) {
                    output.append(byte.toChar())
                    byte = inputStream.read()
                }
            }
        }.join()

        assertEquals("helloworld", output.toString())
    }

    @Test
    fun connectToInputStream() = runTest(timeout = 10.seconds) {
        println("Starting test: connectToInputStream")

        val pipe = Pipe()
        val output = StringBuilder()

        val externalInput = "This is data from external InputStream".byteInputStream()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Connecting external input stream to the pipe
            launch {
                pipe.connectTo(externalInput)
                println("Finished transferring data from external InputStream")
            }
            // Reading from the pipe's InputStream
            launch {
                pipe.inputStream().use { it.copyTo(output.asOutputStream()) }
                println("Finished reading data from pipe's InputStream")
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals("This is data from external InputStream", output.toString())
    }

    @Test
    fun connectToOutputStream() = runTest(timeout = 10.seconds) {
        println("Starting test: connectToOutputStream")

        val pipe = Pipe()
        val externalOutput = StringBuilder()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Writing data to the pipe's OutputStream
            launch {
                pipe.outputStream().use {
                    it.write("Data transferred to external OutputStream".encodeToByteArray())
                }
                println("Finished writing data to the pipe")
            }
            // Connecting the pipe to an external OutputStream
            launch {
                pipe.connectTo(externalOutput.asOutputStream())
                println("Finished transferring data from pipe to external OutputStream")
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals("Data transferred to external OutputStream", externalOutput.toString())
    }

    @Test
    fun connectInputToOutput() = runTest(timeout = 10.seconds) {
        println("Starting test: connectInputToOutput")

        val pipe = Pipe()
        val externalInput = "This is a round trip".byteInputStream()
        val externalOutput = StringBuilder()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // Connecting external input stream to the pipe
            launch {
                pipe.connectTo(externalInput)
                println("Finished transferring data from external InputStream to the pipe")
            }
            // Connecting the pipe to an external output stream
            launch {
                pipe.connectTo(externalOutput.asOutputStream())
                println("Finished transferring data from the pipe to external OutputStream")
            }
        }.join()

        println("Verifying output with assertEquals")
        assertEquals("This is a round trip", externalOutput.toString())
    }
}

