package com.xingpeds.kross.executable

import java.io.InputStream
import java.io.OutputStream

data class ExecutableResult(
    val output: OutputStream?,
    val input: InputStream?,
    val error: InputStream?,
    val waitFor: suspend () -> Int
)

interface Executable {
    enum class StreamSetting {
        Pipe,
        Inherit,
    }

    suspend operator fun invoke(
        name: String,
        args: List<String>,
        input: StreamSetting = StreamSetting.Inherit,
        output: StreamSetting = StreamSetting.Inherit,
        error: StreamSetting = StreamSetting.Inherit,
    ): ExecutableResult
}

class JavaOSProcess : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        input: Executable.StreamSetting,
        output: Executable.StreamSetting,
        error: Executable.StreamSetting
    ): ExecutableResult {

        val pb = ProcessBuilder(listOf(name) + args)
        when (input) {
            Executable.StreamSetting.Pipe -> pb.redirectInput(ProcessBuilder.Redirect.PIPE)
            Executable.StreamSetting.Inherit -> pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        when (output) {
            Executable.StreamSetting.Pipe -> pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
            Executable.StreamSetting.Inherit -> pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        when (error) {
            Executable.StreamSetting.Pipe -> pb.redirectError(ProcessBuilder.Redirect.PIPE)
            Executable.StreamSetting.Inherit -> pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
        val process = pb.start()
        return ExecutableResult(
            output = if (output == Executable.StreamSetting.Pipe) process.outputStream else null,
            input = if (input == Executable.StreamSetting.Pipe) process.inputStream else null,
            error = if (error == Executable.StreamSetting.Pipe) process.errorStream else null
        ) {
            process.waitFor()
        }
    }

}