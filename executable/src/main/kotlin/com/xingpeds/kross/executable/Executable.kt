package com.xingpeds.kross.executable

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias ExecutableResult = suspend () -> Int

//data class ExecutableResult(
//    val output: OutputStream? = null,
//    val input: InputStream? = null,
//    val error: InputStream? = null,
//    val waitFor: suspend () -> Int
//)
data class Pipes(
    val programInput: Pipe? = null,
    val programOutput: Pipe? = null,
    val programError: Pipe? = null,
)

interface Executable {


    /**
     * @param output this is the INPUT to the program being run
     */
    suspend operator fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes = Pipes()
    ): ExecutableResult
}

class JavaOSProcess : Executable {
//    override suspend fun invoke(
//        name: String,
//        args: List<String>,
//        input: Executable.StreamSetting,
//        output: Executable.StreamSetting,
//        error: Executable.StreamSetting
//    ): ExecutableResult {
//
//        val pb = ProcessBuilder(listOf(name) + args)
//        when (input) {
//            Executable.StreamSetting.Pipe -> pb.redirectInput(ProcessBuilder.Redirect.PIPE)
//            Executable.StreamSetting.Inherit -> pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
//        }
//        when (output) {
//            Executable.StreamSetting.Pipe -> pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
//            Executable.StreamSetting.Inherit -> pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
//        }
//        when (error) {
//            Executable.StreamSetting.Pipe -> pb.redirectError(ProcessBuilder.Redirect.PIPE)
//            Executable.StreamSetting.Inherit -> pb.redirectError(ProcessBuilder.Redirect.INHERIT)
//        }
//        val process = pb.start()
//        val thing = process.outputStream
//        return ExecutableResult(
//            output = if (output == Executable.StreamSetting.Pipe) process.outputStream else null,
//            input = if (input == Executable.StreamSetting.Pipe) process.inputStream else null,
//            error = if (error == Executable.StreamSetting.Pipe) process.errorStream else null
//        ) {
//            process.waitFor()
//        }
//    }

    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes
    ): ExecutableResult {

        val pb = ProcessBuilder(listOf(name) + args)
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
                }
            }

            launch {

                if (pipes.programOutput != null) {
                    pipes.programOutput.connectTo(process.inputStream)
                }
            }
            launch {
                if (pipes.programError != null) {
                    pipes.programError.connectTo(process.errorStream)
                }
            }
        }
        return {
            process.waitFor()
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
