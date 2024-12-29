package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.*
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File

class Executor(
    private val cwd: StateFlow<File>,
    private val makeExecutable: suspend (name: String) -> Executable,
    private val pipes: Pipes = Pipes(),
    private val shellState: ShellState = ShellStateObject
) {

    private val results = mutableListOf<Int>()
    suspend fun execute(ast: AST.Program): List<Int> {
        ast.commands.forEach { command ->
            exeCommand(command)
        }
        return results
    }

    private suspend fun exeCommand(command: AST.Command): Int {
        return when (command) {
            is AST.Command.And -> exeAnd(command)
            is AST.Command.Or -> exeOr(command)
            is AST.Command.Pipeline -> exePipeline(command)
        }
    }

    private suspend fun exeOr(command: AST.Command.Or): Int {
        val left = exeCommand(command.left)
        if (left == 0) {
            return 0
        }
        return exeCommand(command.right)
    }

    private suspend fun exeAnd(command: AST.Command.And): Int {
        val left = exeCommand(command.left)
        if (left == 0) {
            return exeCommand(command.right)
        }
        return left
    }

    private suspend fun exePipeline(pipeline: AST.Command.Pipeline): Int {
        val commands = pipeline.commands
        return if (commands.size == 1) {
            exeSimpleCommand(commands.first())
        } else if (commands.size == 2) {
            val pipe = Chan()
            var returnCode = 99
            coroutineScope {
                launch {
                    require(pipe.isClosedForSend.not()) { " pipe is closed before first program to output" }
                    val first = exeSimpleCommand(commands.first(), pipes.copy(programOutput = pipe))
                }
                launch {
                    require(pipe.isClosedForSend.not()) { " pipe is closed before second program to input" }
                    returnCode = exeSimpleCommand(commands.last(), pipes.copy(programInput = pipe))
                }
            }.join()
            pipe.close()
            return returnCode
        } else {
            coroutineScope {
                val pipelist = mutableListOf<Channel<Int>>()
                val jobs = mutableListOf<Job>()
                for ((index, command) in commands.withIndex()) {
                    when (index) {
                        0 -> {
                            val pipe = Chan()
                            pipelist.add(pipe)
                            jobs += launch {
                                exeSimpleCommand(command, pipes.copy(programOutput = pipe))
                            }
                        }

                        commands.lastIndex -> {
                            yield()
                            val previousPipe = pipelist.last()
                            val code = exeSimpleCommand(command, pipes.copy(programInput = previousPipe))
                            jobs.forEach { it.join() }
                            pipelist.forEach { it.close() }
                            return@coroutineScope code
                        }

                        else -> {
                            // middle process
                            yield()
                            jobs.last().join()
                            val previousPipe = pipelist.last()
                            val pipe = Chan()
                            pipelist.add(pipe)
                            jobs += launch {
                                exeSimpleCommand(
                                    command,
                                    pipes.copy(programOutput = pipe, programInput = previousPipe)
                                )
                            }
                        }
                    }
                }
                -9999 //should never get here
            }
        }
    }

    private suspend fun exeSimpleCommand(command: AST.SimpleCommand, pipes: Pipes = this.pipes): Int {
        val commandName = command.name.value
        return try {
            val executable = makeExecutable(commandName)
            val resolvedArguments: List<String> = command.arguments.map { arg ->
                when (arg) {
                    is AST.Argument.CommandSubstitution -> exeCommandSub(arg)
                    is AST.Argument.VariableSubstitution -> this.shellState.environment.value[arg.variableName] ?: ""
                    is AST.Argument.WordArgument -> arg.value
                }
            }
            executable(
                commandName,
                resolvedArguments,
                pipes,
                shellState.environment.value,
                cwd.value
            )().also { results.add(it) }
        } catch (e: Exception) {
            e.error("$commandName failed to run")
            -99
        }
    }

    private suspend fun exeCommandSub(arg: AST.Argument.CommandSubstitution): String {
        val output = StringBuilder()
        val pipe = SupervisorChannel()
        val inPipe = Chan()
        inPipe.close()
        coroutineScope {
            launch {
                val executor =
                    Executor(
                        cwd,
                        makeExecutable,
                        shellState = shellState,
                        pipes = Pipes(programOutput = pipe, programInput = inPipe)
                    )
                val codes = executor.execute(arg.commandLine)
                results.addAll(codes)
                codes.debug("subcommand return codes")
                pipe.superClose()
            }
            launch {
                pipe.connectTo(output.asOutputStream(), name = "subcommand output pipe")

            }
        }
        Log.info("return subcommand")

        return output.toString().trim()
    }
}
