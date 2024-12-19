package com.xingpeds.kross.parser

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipe
import com.xingpeds.kross.executable.Pipes
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream


typealias BuiltinCommand = suspend (args: List<String>) -> Int

class Executor(
    private val cwd: StateFlow<File>,
    private val makeExecutable: suspend (name: String) -> Executable,
    private val pipes: Pipes = Pipes(),
    private val shellState: ShellState = ShellStateObject
) {
    private val results = mutableListOf<Int>()

    data class Streams(
        val inputStream: InputStream? = null,
        val outputStream: OutputStream? = null,
        val errorStream: OutputStream? = null,
    )

    suspend fun execute(
        ast: AST.Program,
    ): List<Int> {

        for (command in ast.commands) {
            exeCommand(command)
        }
        pipes.programInput?.close()
        pipes.programOutput?.close()
        pipes.programError?.close()
        return results
    }

    suspend fun exeCommand(command: AST.Command): Int {
        return when (command) {
            is AST.Command.And -> exeAnd(command)
            is AST.Command.Or -> exeOr(command)
            is AST.Command.Pipeline -> exePipeline(command)
        }
    }

    private suspend fun exeOr(command: AST.Command.Or): Int {
        val left = exeCommand(command.left)
        if (left != 0) {
            val right = exeCommand(command.right)
            return left * right
        }
        return left
    }

    private suspend fun exePipeline(
        pipeline: AST.Command.Pipeline,
    ): Int = coroutineScope {

        val commands = pipeline.commands
        if (pipeline.commands.size == 1) {
            val command = pipeline.commands.first()
            return@coroutineScope exeExternalProcess(
                name = command.name.value,
                arguments = command.arguments,
                pipes = pipes
            )().also { results.add(it) }
        } else if (commands.size == 2) {
            val first = commands.first()
            val second = commands.last()
            val pipe = Pipe()
            val firstPipes = this@Executor.pipes.copy(programOutput = pipe)
            val secondPipes = this@Executor.pipes.copy(programInput = pipe)
            exeExternalProcess(
                name = first.name.value,
                arguments = first.arguments,
                pipes = firstPipes
            )().also { results.add(it) }
            pipe.close()
            exeExternalProcess(
                name = second.name.value,
                arguments = second.arguments,
                pipes = secondPipes
            )().also { results.add(it) }
        } else {
            //handle size of n pipeline
            val pipeList = mutableListOf<Pipe>()
            val waiting = mutableListOf<ExecutableResult>()
            for ((index, command) in pipeline.commands.withIndex()) {
                when (index) {
                    0 -> {
                        // handle the first command
                        println("first element")
                        val pipe = Pipe()
                        pipeList.add(pipe)
                        val pipes = this@Executor.pipes.copy(programOutput = pipe)
                        val result = exeExternalProcess(
                            name = command.name.value,
                            arguments = command.arguments,
                            pipes = pipes
                        )
                        waiting.add(result)
                    }

                    pipeline.commands.lastIndex -> {
                        //handle the last element
                        println("last element")
                        val pipe = pipeList.last()
                        val previous = waiting.last()
                        val pipes = this@Executor.pipes.copy(programInput = pipe)
                        coroutineScope {
                            launch {
                                println("exe last element")
                                waiting.add(
                                    exeExternalProcess(
                                        name = command.name.value,
                                        arguments = command.arguments,
                                        pipes = pipes
                                    )
                                )
                            }
                            launch {
                                println("waiting for middle to finish")
                                previous().also { results.add(it) }

                                println("closing middle pipe")
                                pipe.close()
                            }
                        }
                        println("waiting for final to finish")
                        return@coroutineScope waiting.last()().also { results.add(it) }
                    }

                    else -> {
                        //hand a middle element

                        val pipe = pipeList.last()
                        val previousResult = waiting.last()
                        val nextPipe = Pipe()
                        val pipes = this@Executor.pipes.copy(programInput = pipe, programOutput = nextPipe)
                        pipeList.add(nextPipe)
                        println("starting middle command")
                        coroutineScope {
                            launch {
                                println("exe middle command")
                                waiting.add(
                                    exeExternalProcess(
                                        name = command.name.value,
                                        arguments = command.arguments,
                                        pipes = pipes
                                    )
                                )
                            }
                            launch {
                                println("waiting for previous to finish")
                                previousResult().also { results.add(it) }
                                pipe.close()
                            }
                        }
                    }
                }
            }
            0
        }

    }

    private suspend fun exeSimpleCommand(
        command: AST.SimpleCommand,
        pipes: Pipes,
        env: Map<String, String>,
    ): ExecutableResult {

        return exeExternalProcess(command.name.value, command.arguments, pipes)
    }

    private suspend fun exeExternalProcess(
        name: String,
        arguments: List<AST.Argument>,
        pipes: Pipes,
    ): ExecutableResult = coroutineScope {
        val env = shellState.environment.value
        val resolvedArguments: List<String> = arguments.map { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> exeSubCommand(arg)
                is AST.Argument.VariableSubstitution -> env[arg.variableName] ?: ""
                is AST.Argument.WordArgument -> arg.value
            }
        }
        val executable = makeExecutable(name)
        val finish = executable(name, resolvedArguments, pipes = pipes, env = env, cwd = cwd.value)
        finish
    }

    private suspend fun exeSubCommand(arg: AST.Argument.CommandSubstitution): String {
        val output = StringBuilder()
        val pipes = Pipes(
            programOutput = Pipe(),
            programInput = Pipe()
        )
        pipes.programOutput?.connectTo(output.asOutputStream())
        val executor = Executor(cwd, pipes = pipes, makeExecutable = this.makeExecutable, shellState = this.shellState)

        executor.execute(ast = arg.commandLine)
        return output.toString().trim()
    }


    suspend fun exeAnd(command: AST.Command.And): Int {
        val result: Int = exeCommand(command.left)
        if (result == 0) {
            return exeCommand(command.right)
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
