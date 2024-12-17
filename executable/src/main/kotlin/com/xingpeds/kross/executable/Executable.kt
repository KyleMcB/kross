package com.xingpeds.kross.executable

import kotlinx.coroutines.coroutineScope
import java.io.InputStream
import java.io.OutputStream

data class ExecutableResult(
    val output: OutputStream? = null,
    val input: InputStream? = null,
    val error: InputStream? = null,
    val waitFor: suspend () -> Int
)

interface Executable {
    enum class StreamSetting {
        Pipe,
        Inherit,
    }

    /**
     * @param output this is the INPUT to the program being run
     */
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
        val thing = process.outputStream
        return ExecutableResult(
            output = if (output == Executable.StreamSetting.Pipe) process.outputStream else null,
            input = if (input == Executable.StreamSetting.Pipe) process.inputStream else null,
            error = if (error == Executable.StreamSetting.Pipe) process.errorStream else null
        ) {
            process.waitFor()
        }
    }

}

suspend fun InputStream.copyToSuspend(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
    coroutineScope() {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        while (read(buffer).also { bytesRead = it } >= 0) {
            println(buffer.contentToString())
            out.write(buffer, 0, bytesRead)
            out.flush()
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
