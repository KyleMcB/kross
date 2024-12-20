package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST
import com.xingpeds.kross.entities.Chan
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

private fun log(any: Any) = println("[Executor] $any")

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
                    val first = exeSimpleCommand(commands.first(), pipes.copy(programOutput = pipe))
                    pipe.close()
                }
                launch {
                    returnCode = exeSimpleCommand(commands.last(), pipes.copy(programInput = pipe))
                }
            }
            return returnCode
        } else {
            TODO()
        }
    }

    private suspend fun exeSimpleCommand(command: AST.SimpleCommand, pipes: Pipes = this.pipes): Int {
        log("exe simple command: $command")
        val executable = makeExecutable(command.name.value)
        val resolvedArguments: List<String> = command.arguments.map { arg ->
            when (arg) {
                is AST.Argument.CommandSubstitution -> TODO()
                is AST.Argument.VariableSubstitution -> this.shellState.environment.value[arg.variableName] ?: ""
                is AST.Argument.WordArgument -> arg.value
            }
        }
        return executable(
            command.name.value,
            resolvedArguments,
            pipes,
            shellState.environment.value,
            cwd.value
        )().also { results.add(it) }
    }
}
