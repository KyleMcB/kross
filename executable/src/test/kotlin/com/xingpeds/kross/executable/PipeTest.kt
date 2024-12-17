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
}

