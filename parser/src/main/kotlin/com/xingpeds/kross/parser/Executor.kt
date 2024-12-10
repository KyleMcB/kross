package com.xingpeds.kross.parser

import com.xingpeds.kross.parser.Executor.StreamSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

fun settingsFromStreams(streams: Executor.Streams): Executor.StreamSettings {
    val default = StreamSettings()
    return StreamSettings(
        input = if (streams.inputStream != null) ProcessBuilder.Redirect.PIPE else default.input,
        output = if (streams.outputStream != null) ProcessBuilder.Redirect.PIPE else default.output,
        error = if (streams.errorStream != null) ProcessBuilder.Redirect.PIPE else default.error,
    )
}

class Executor(private val streamOverrides: Streams = Streams()) {
    private val results = mutableListOf<Int>()

    data class StreamContext(
        val streams: Streams,
        val settings: StreamSettings = settingsFromStreams(streams),
    ) {

    }

    data class Streams(
        val inputStream: InputStream? = null,
        val outputStream: OutputStream? = null,
        val errorStream: OutputStream? = null,
    )


    suspend fun execute(
        ast: AST.Program,
        streams: Streams = Streams(),
        env: Map<String, String> = emptyMap()
    ): List<Int> {

        for (command in ast.commands) {
            exeCommand(command, StreamContext(streams = streams), env)
        }
        streams.inputStream?.close()
        streams.outputStream?.close()
        streams.errorStream?.close()
        return results
    }

    suspend fun exeCommand(command: AST.Command, streams: StreamContext, env: Map<String, String>): Int {
        return when (command) {
            is AST.Command.And -> exeAnd(command, streams, env)
            is AST.Command.Or -> exeOr(command, streams, env)
            is AST.Command.Pipeline -> exePipeline(command, streams, env)
        }
    }

    private suspend fun exeOr(command: AST.Command.Or, streams: StreamContext, env: Map<String, String>): Int {
        val left = exeCommand(command.left, streams, env)
        if (left != 0) {
            val right = exeCommand(command.right, streams, env)
            return left * right
        }
        return left
    }

    private suspend fun exePipeline(
        pipeline: AST.Command.Pipeline,
        sc: StreamContext,
        env: Map<String, String>
    ): Int = coroutineScope {
        val streamsJobs = mutableListOf<Job>()
        if (pipeline.commands.size == 1) {
            val process = exeSimpleCommand(pipeline.commands.first(), sc.settings, env)
            streamsJobs.add(launch {
                sc.streams.outputStream?.let { process.output?.copyToSuspend(it) }
                process.output?.close()
            })
            if (sc.streams.inputStream != null && process.input != null) {
                streamsJobs.add(launch {
                    sc.streams.inputStream.copyToSuspend(process.input)
                    process.input.close()
                })
            }
            streamsJobs.forEach { it.join() }
            process.finish()
        } else {

            // lets assume only two commands for now
            // I need to buffer the output of the first one

            val secondJob = exeSimpleCommand(
                pipeline.commands[1],
                streams = sc.settings.copy(input = ProcessBuilder.Redirect.PIPE),
                env = env
            )
            streamsJobs.add(launch {
                sc.streams.outputStream?.let { secondJob.output?.copyToSuspend(it) }
                secondJob.output?.close()
            })
            val firstJob =
                exeSimpleCommand(
                    pipeline.commands.first(),
                    streams = sc.settings.copy(output = ProcessBuilder.Redirect.PIPE),
                    env = env
                )
            streamsJobs.add(launch {
                secondJob.input?.let { firstJob.output?.copyToSuspend(it) }
                firstJob.output?.close()
                secondJob.input?.close()
            })
            streamsJobs.forEach { it.join() }
            firstJob.finish()
            val result = secondJob.finish()
            result
        }
    }

    fun bufferStream(buffer: StringBuffer): InputStream = object : InputStream() {
        private var position = 0

        override fun read(): Int {
            if (position >= buffer.length) {
                return -1
            }
            return buffer[position++].code

        }
    }

    fun StringBuffer.asOutputStream(): OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            append(b.toChar())
        }

    }

    private suspend fun exeSimpleCommand(
        command: AST.SimpleCommand,
        streams: StreamSettings,
        env: Map<String, String>,
    ): ProcessResult {
        return when (command.name) {
            is AST.CommandName.Path -> exeExternalProcess(command.name.value, command.arguments, streams, env)
            is AST.CommandName.Word -> {
                if (internalCommandsContains(command.name.value)) {
                    exeInternalCommandsContains(command.name.value, command.arguments, streams, env)
                } else {
                    exeExternalProcess(command.name.value, command.arguments, streams, env)
                }
            }
        }
    }

    private suspend fun exeInternalCommandsContains(
        name: String,
        arguments: List<AST.Argument>,
        streams: StreamSettings,
        env: Map<String, String>
    ): ProcessResult {
        TODO()
    }

    data class StreamSettings(
        val input: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT,
        val output: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT,
        val error: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT,
    )

    data class ProcessResult(
        val output: InputStream? = null,
        val input: OutputStream? = null,
        val error: InputStream? = null,
        val finish: () -> Int,
    )

    private fun internalCommandsContains(name: String): Boolean {
        return false
    }

    private suspend fun exeExternalProcess(
        name: String,
        arguments: List<AST.Argument>,
        streams: StreamSettings,
        env: Map<String, String>
    ): ProcessResult = coroutineScope {

        val resolvedArguments: List<String> = arguments.map { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> TODO()
                is AST.Argument.VariableSubstitution -> env[arg.variableName] ?: ""
                is AST.Argument.WordArgument -> arg.value
            }
        }
        val list = listOf(name) + resolvedArguments
        val pb = ProcessBuilder(list)
        pb.redirectInput(streams.input)
        pb.redirectOutput(streams.output)
        pb.redirectError(streams.error)
        val process = pb.start()
        ProcessResult(
            output = if (streams.output == ProcessBuilder.Redirect.PIPE) process.inputStream else null,
            input = if (streams.input == ProcessBuilder.Redirect.PIPE) process.outputStream else null,
            error = if (streams.error == ProcessBuilder.Redirect.PIPE) process.errorStream else null
        ) {
            val result = process.waitFor()
            results.add(result)
            process.destroy()
            if (streams.output == ProcessBuilder.Redirect.PIPE) {
                process.outputStream.close()
            }
            if (streams.input == ProcessBuilder.Redirect.PIPE) {
                process.inputStream.close()
            }
            if (streams.error == ProcessBuilder.Redirect.PIPE) {
                process.errorStream.close()
            }
            result
        }
    }

    suspend fun exeAnd(command: AST.Command.And, streams: StreamContext, env: Map<String, String>): Int {
        val result: Int = exeCommand(command.left, streams, env)
        if (result == 0) {
            return exeCommand(command.right, streams, env)
        }
        return result
    }
}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}

fun StringBuilder.asInputStream(): InputStream = object : InputStream() {
    private var position = 0 // Tracks the current read position

    override fun read(): Int {
        // If the position is beyond the StringBuilder length, return -1 (end of stream)
        if (position >= this@asInputStream.length) {
            return -1
        }
        // Return the character at the current position as an integer and advance the position
        return this@asInputStream[position++].code
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // If no more data, return -1 (end of stream)
        if (position >= this@asInputStream.length) {
            return -1
        }

        // Calculate how many bytes we can read
        val bytesToRead = minOf(len, this@asInputStream.length - position)
        for (i in 0 until bytesToRead) {
            b[off + i] = this@asInputStream[position++].code.toByte()
        }
        return bytesToRead
    }
}

suspend fun InputStream.copyToSuspend(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
    coroutineScope() {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        while (read(buffer).also { bytesRead = it } > 0) {
            out.write(buffer, 0, bytesRead)
            out.flush()
        }
    }
}