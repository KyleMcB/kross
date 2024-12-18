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
        env: Map<String, String>
    ): ExecutableResult
}

class JavaOSProcess : Executable {

    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>
    ): ExecutableResult {

        val pb = ProcessBuilder(listOf(name) + args)
        pb.environment().clear()
        pb.environment().putAll(env)
        if (pipes.programInput != null) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programOutput != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programError != null) {
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
        val process = pb.start()
        coroutineScope {
            launch {
                if (pipes.programInput != null) {
                    pipes.programInput.connectTo(process.outputStream)
                    process.outputStream.close()
                }
            }
            launch {
                if (pipes.programOutput != null) {
                    pipes.programOutput.connectTo(process.inputStream)
                    process.inputStream.close()
                }
            }
            launch {
                if (pipes.programError != null) {
                    pipes.programError.connectTo(process.errorStream)
                    process.errorStream.close()
                }
            }
        }
        return {
            val exitCode = process.waitFor()
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
