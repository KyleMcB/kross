package com.xingpeds.kross.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class Executor {
    private val results = mutableListOf<Int>()

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
        println("Entering execute with ast: $ast, streams: $streams, env: $env")
        for (command in ast.commands) {
            exeCommand(command, streams, env)
        }
        return results
    }

    suspend fun exeCommand(command: AST.Command, streams: Streams, env: Map<String, String>): Int {
        println("Entering exeCommand with command: $command, streams: $streams, env: $env")
        return when (command) {
            is AST.Command.And -> exeAnd(command, streams, env)
            is AST.Command.Or -> exeOr(command, streams, env)
            is AST.Command.Pipeline -> exePipeline(command, streams, env)
        }
    }

    private suspend fun exeOr(command: AST.Command.Or, streams: Streams, env: Map<String, String>): Int {
        println("Entering exeOr with command: $command, streams: $streams, env: $env")
        val left = exeCommand(command.left, streams, env)
        if (left != 0) {
            println("Condition check in exeOr: left != 0")
            val right = exeCommand(command.right, streams, env)
            return left * right
        }
        return left
    }

    private suspend fun exePipeline_old2(
        pipeline: AST.Command.Pipeline,
        streams: Streams,
        env: Map<String, String>
    ): Int {
        println("Entering exePipeline with pipeline: $pipeline, streams: $streams, env: $env")
        val streamHolder = mutableListOf<Streams>()
        val jobs = mutableListOf<ProcessResult>()

        // Proper handling of the first command in the pipeline
        val firstJob = exeSimpleCommand(pipeline.commands.first(), streams, env)
        jobs.add(firstJob)
        streamHolder.add(Streams(inputStream = firstJob.output))

        // Adjust the loop to cover each command sequentially
        for (index in 1 until pipeline.commands.lastIndex) {
            println("Entering loop in exePipeline with index: $index")
            val job = exeSimpleCommand(pipeline.commands[index], streamHolder.last(), env)
            jobs.add(job)
            streamHolder.add(Streams(inputStream = job.output))
        }

        // Handle the last command in the list
        val lastStreams = streams.copy(inputStream = streamHolder.last().inputStream)
        val lastJob = exeSimpleCommand(pipeline.commands.last(), lastStreams, env)
        jobs.add(lastJob)

        // Ensure each process is correctly terminated and its result collected
        return jobs.map { it.finish() }.last()
    }

    private suspend fun exePipeline(
        pipeline: AST.Command.Pipeline,
        streams: Streams,
        env: Map<String, String>
    ): Int {
        if (pipeline.commands.size == 1) {
            return exeSimpleCommand(pipeline.commands.first(), streams, env).finish()
        }
        // lets assume only two commands for now
        // I need to buffer the output of the first one
        println("wtf")
        val buffer = StringBuffer()

        val firstJob =
            exeSimpleCommand(pipeline.commands.first(), streams.copy(outputStream = buffer.asOutputStream()), env)
        firstJob.finish()
        val secondJob = exeSimpleCommand(pipeline.commands[1], streams.copy(inputStream = bufferStream(buffer)), env)
        val result = secondJob.finish()
        return 0
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
        streams: Streams,
        env: Map<String, String>,
    ): ProcessResult {
        println("Entering exeSimpleCommand with command: $command, streams: $streams, env: $env")
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
        streams: Streams,
        env: Map<String, String>
    ): ProcessResult {
        TODO()
    }

    data class ProcessResult(
        val output: InputStream,
        val finish: () -> Int,
    )

    private fun internalCommandsContains(name: String): Boolean {
        return false
    }

    private suspend fun exeExternalProcess(
        name: String,
        arguments: List<AST.Argument>,
        streams: Streams,
        env: Map<String, String>
    ): ProcessResult = coroutineScope {
        println("Entering exeExternalProcess with name: $name, arguments: $arguments, streams: $streams, env: $env")

        val resolvedArguments: List<String> = arguments.map { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> TODO()
                is AST.Argument.VariableSubstitution -> env[arg.variableName] ?: ""
                is AST.Argument.WordArgument -> arg.value
            }
        }
        val list = listOf(name) + resolvedArguments
        val pb = ProcessBuilder(list)
        if (streams.errorStream != null) {
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
        if (streams.outputStream != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        if (streams.inputStream != null) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        val process = pb.start()
        if (streams.inputStream != null) {
            launch {
                streams.inputStream.copyToSuspend(process.outputStream)
                println("closing input stream of $name")
                process.outputStream.close()
            }
        }
        if (streams.outputStream != null) {
            launch {
                process.inputStream.copyToSuspend(streams.outputStream)
                println("closing output stream of $name")
                process.inputStream.close()
            }
        }
        if (streams.errorStream != null) {
            launch {
                process.errorStream.copyToSuspend(streams.errorStream)
                println("closing error stream of $name")
                process.errorStream.close()
            }
        }
        ProcessResult(output = process.inputStream) {
            val result = process.waitFor()
            results.add(result)
            process.destroy()
            result
        }
    }

    suspend fun exeAnd(command: AST.Command.And, streams: Streams, env: Map<String, String>): Int {
        println("Entering exeAnd with command: $command, streams: $streams, env: $env")
        val result: Int = exeCommand(command.left, streams, env)
        if (result == 0) {
            println("Condition check in exeAnd: result == 0")
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
    withContext(Dispatchers.IO) {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        while (read(buffer).also { bytesRead = it } > 0) {
            println("copying data in streams")
            out.write(buffer, 0, bytesRead)
            out.flush()
        }
        println("closing stream in copier")
        close()
    }
}