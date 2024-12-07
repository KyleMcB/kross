package com.xingpeds.kross.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class Executor {
    data class Stream(
        val inputStream: InputStream? = null,
        val outputStream: OutputStream? = null,
        val errorStream: OutputStream? = null,
    )

    suspend fun execute(ast: AST.Program, stream: Stream = Stream(), env: Map<String, String> = emptyMap()) {
        for (command in ast.commands) {
            exeCommand(command, stream, env)
        }
    }

    suspend fun exeCommand(command: AST.Command, stream: Stream, env: Map<String, String>) {
        when (command) {
            is AST.Command.And -> exeAnd(command, stream, env)
            is AST.Command.Or -> TODO()
            is AST.Command.Pipeline -> exePipeline(command, stream, env)
        }
    }

    private suspend fun exePipeline(pipeline: AST.Command.Pipeline, stream: Stream, env: Map<String, String>) {
        if (pipeline.commands.size == 1) {
            exeSimpleCommand(pipeline.commands.first(), stream, env).finish()
        }
    }

    private suspend fun exeSimpleCommand(
        command: AST.SimpleCommand,
        stream: Stream,
        env: Map<String, String>
    ): ProcessResult {
        return when (command.name) {
            is AST.CommandName.Path -> exeExternalProcess(command.name.value, command.arguments, stream, env)
            is AST.CommandName.Word -> {
                if (internalCommandsContains(command.name.value)) {
                    exeInternalCommandsContains(command.name.value, command.arguments, stream, env)
                } else {
                    exeExternalProcess(command.name.value, command.arguments, stream, env)
                }
            }
        }
    }

    private suspend fun exeInternalCommandsContains(
        name: String,
        arguments: List<AST.Argument>,
        stream: Stream,
        env: Map<String, String>
    ): ProcessResult {
        TODO()
    }

    data class ProcessResult(
        val finish: () -> Int,
        val output: InputStream,
    )

    private fun internalCommandsContains(name: String): Boolean {
        return false
    }

    private suspend fun exeExternalProcess(
        name: String,
        arguments: List<AST.Argument>,
        stream: Stream,
        env: Map<String, String>
    ): ProcessResult {

        val resolvedArguments: List<String> = arguments.map { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> TODO()
                is AST.Argument.VariableSubstitution -> env[arg.variableName] ?: ""
                is AST.Argument.WordArgument -> arg.value
            }
        }
        println(listOf(name) + resolvedArguments)
        val pb = ProcessBuilder(listOf(name) + resolvedArguments)
        pb.inheritIO()
        val process = pb.start()
        return ProcessResult(output = process.inputStream, finish = process::waitFor)
    }

    suspend fun exeAnd(command: AST.Command.And, stream: Stream, env: Map<String, String>) {

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
        while (read(buffer).also { bytesRead = it } >= 0) {
            out.write(buffer, 0, bytesRead)
            out.flush()
        }
    }
}