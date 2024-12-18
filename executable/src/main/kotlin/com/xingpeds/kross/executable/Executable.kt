package com.xingpeds.kross.executable

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias ExecutableResult = suspend () -> Int


data class Pipes(
    val programInput: Pipe? = null,
    val programOutput: Pipe? = null,
    val programError: Pipe? = null,
)

interface Executable {

    suspend operator fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes = Pipes(),
        env: Map<String, String> = emptyMap()
    ): ExecutableResult
}

class JavaOSProcess : Executable {

    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>
    ): ExecutableResult {

        println("Entering invoke method")
        println("Executable name: $name")
        println("Executable arguments: $args")
        println("Pipes: $pipes")
        println("Environment variables: $env")
        val pb = ProcessBuilder(listOf(name) + args)
        pb.environment().clear()
        println("Cleared existing environment variables")
        pb.environment().putAll(env)
        println("Set environment variables: ${pb.environment()}")
        if (pipes.programInput != null) {
            println("Redirecting program input to PIPE")
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
        } else {
            println("Inheriting program input")
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programOutput != null) {
            println("Redirecting program output to PIPE")
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        } else {
            println("Inheriting program output")
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programError != null) {
            println("Redirecting program error to PIPE")
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
        } else {
            println("Inheriting program error")
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
        println("Starting the process with ProcessBuilder")
        val process = pb.start()
        println("Process started")
        coroutineScope {
            launch {
                if (pipes.programInput != null) {
                    println("Connecting program input to process output stream")
                    pipes.programInput.connectTo(process.outputStream)
                    println("Program input connected")
                }
            }
            launch {
                if (pipes.programOutput != null) {
                    println("Connecting program output to process input stream")
                    pipes.programOutput.connectTo(process.inputStream)
                    println("Program output connected")
                }
            }
            launch {
                if (pipes.programError != null) {
                    println("Connecting program error to process error stream")
                    pipes.programError.connectTo(process.errorStream)
                    println("Program error connected")
                }
            }
        }
        println("Returning ExecutableResult after process start")
        return {
            println("Waiting for process to complete")
            val exitCode = process.waitFor()
            println("Process completed with exit code: $exitCode")
            exitCode
        }
    }

}

fun StringBuilder.asOutputStream(): java.io.OutputStream {
    return object : java.io.OutputStream() {
        override fun write(b: Int) {
            append(b.toChar())
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            append(String(b, off, len))
        }
    }
}
