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
import java.nio.file.FileSystems

val singleDollarRegex = Regex("""(?<!\\)\$[a-zA-Z_][a-zA-Z0-9_]*""")

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
                                    command, pipes.copy(programOutput = pipe, programInput = previousPipe)
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
        val commandName = command.name.identifier
        val executable = makeExecutable(commandName)
        val resolvedArguments = command.arguments.flatMap<AST.Argument, String> { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> listOf(exeCommandSub(arg))
                is AST.Argument.VariableSubstitution -> listOf(
                    this.shellState.environment.value[arg.variableName] ?: ""
                )
                // FIXME I just found out the shell is responsible for text replacing the ~ with the home dire
                // not sure if this is the right place for that
                is AST.Argument.WordArgument -> {
                    val text = arg.value
                    if (text.startsWith("~")) {
                        listOf(text.replaceFirst("~", System.getProperty("user.home")))
                    } else listOf(arg.value)
                }

                is AST.Argument.DoubleQuoteWithVar -> listOf(expandDoubleQuoteWithVar(arg))
                is AST.Argument.Glob -> expandGlobArgument(arg)
                is AST.Argument.RecursiveGlob -> expandRecursiveGlob(arg)
            }
        }.toList()
        return executable(
            commandName, resolvedArguments, pipes, shellState.environment.value, cwd.value
        )().also { results.add(it) }
    }

    private suspend fun expandRecursiveGlob(arg: AST.Argument.RecursiveGlob): Iterable<String> {
        fun listAllFilesRecursively(file: File): List<File> {
            return file.listFiles()?.flatMap {
                if (it.isDirectory) listAllFilesRecursively(it) + it
                else listOf(it)
            } ?: emptyList()
        }

        val pattern = arg.text // The glob pattern, e.g., "*.txt"
        val cwdFile = cwd.value // The current working directory

        // Get the list of files in the current directory
        val allFilesRecursive = listAllFilesRecursively(cwdFile)
        val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
        // Filter files by matching the filenames to the glob pattern
        return coroutineScope {
            allFilesRecursive.parallelMap(this) { file ->
                if (pathMatcher.matches(file.toPath().fileName)) {
                    file.canonicalPath.replace(cwdFile.canonicalPath + "/", "")
                } else null
            }.filterNotNull()
        }
    }


    private fun expandGlobArgument(arg: AST.Argument.Glob): Collection<String> {
        val pattern = arg.text // The glob pattern, e.g., "*.txt"
        val cwdFile = cwd.value // The current working directory
        val normalizedPattern = if (pattern.startsWith("./")) pattern.substring(2) else pattern

        // Get the list of files in the current directory
        val filesInCwd = cwdFile.listFiles()?.filter { !it.isHidden } ?: emptyList()
        val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$normalizedPattern")
        // Filter files by matching the filenames to the glob pattern
        val matches = filesInCwd.filter { pathMatcher.matches(it.toPath().fileName) }.map { it.name }
        return matches
    }


    private suspend fun expandDoubleQuoteWithVar(arg: AST.Argument.DoubleQuoteWithVar): String {
        var text = arg.text
        val env = this.shellState.environment.value
        val wrappedLocations = wrappedDollarLocations(text)
        wrappedLocations.forEach { wrapperVar ->
            val varName = wrapperVar.substring(2, wrapperVar.length - 1)
            val value = env[varName] ?: "null"
            text = text.replace(wrapperVar, value)
        }
        return singleDollarRegex.findAll(text).toList().reversed().fold(text) { acc, matchResult ->
            val varName = matchResult.value.drop(1)
            val value = env[varName]
            if (value == null) {
                acc
            } else {
                acc.replace(matchResult.value, value)
            }
        }
    }

    private suspend fun exeCommandSub(arg: AST.Argument.CommandSubstitution): String {
        val output = StringBuilder()
        val pipe = SupervisorChannel()
        val inPipe = Chan()
        inPipe.close()
        coroutineScope {
            launch {
                val executor = Executor(
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
        return output.toString().trim()
    }
}

fun wrappedDollarLocations(text: String): List<String> {
    val wrappedRegex = Regex("(?<!\\\\)\\$\\{([^}]+)}")
    return wrappedRegex.findAll(text).map {
        it.value
    }.toList()
}
