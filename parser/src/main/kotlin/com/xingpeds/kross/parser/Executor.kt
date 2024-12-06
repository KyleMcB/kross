package com.xingpeds.kross.parser

import java.io.InputStream
import java.io.OutputStream

// this probably should not be in the parser module, but I'll deal with that later
class Executor(
    private val program: AST.Program,
    private val input: InputStream? = null,  // Stream to provide input to the process
    private val output: OutputStream? = null, // Stream to capture the process's output
    private val error: OutputStream? = null, // Stream to capture the process's error output
    private val env: Map<String, String> = emptyMap()
) {
    fun execute() {
        executeProgram(program)
    }

    private fun executeProgram(program: AST.Program) {
        for (statement in program.statements) {
            exeStatment(statement)
        }
    }

    private fun exeStatment(statement: AST.Command) {
        when (statement) {
            is AST.And -> TODO()
            is AST.Or -> TODO()
            is AST.Pipeline -> pipeline(statement)
            is AST.SimpleCommand -> simpleCommand(statement)
        }
    }

    private fun pipeline(pipeline: AST.Pipeline) {
        val commandList = pipeline.commands
        commandList.forEachIndexed { index, command ->
            when (index) {
                0 -> {
                    // first one inherits our input
                }

                commandList.lastIndex -> {
                    // last one gets out output
                }

                else -> {
                    // middle ones get input and output redirected
                }
            }
        }
    }

    fun simpleCommand(
        command: AST.SimpleCommand,
        input: InputStream? = this.input,  // Stream to provide input to the process
        output: OutputStream? = this.output, // Stream to capture the process's output
        error: OutputStream? = this.error  // Stream to capture the process's error output
    ): Int {
        val args = command.arguments.map { argument: AST.Argument ->
            when (argument) {
                is AST.CommandSubstitution -> commandSubtitution(argument.commandLine)
                is AST.VariableSubstitution -> variableSubstitution(argument)
                is AST.WordArgument -> argument.value
            }
        }
        val builder = ProcessBuilder(listOf(command.name) + args)
        val pbenv: MutableMap<String, String> = builder.environment()

        pbenv.putAll(env)
        // Set default redirections if no custom streams are provided
        if (input == null) builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
        if (output == null) builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        if (error == null) builder.redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = builder.start()

        // Handle input redirection
        input?.let { source ->
            process.outputStream.use { dest ->
                source.copyTo(dest) // Copy data from input stream to process input
            }
        } ?: process.outputStream.close() // Close if no input is provided

        // Handle output redirection
        output?.let { dest ->
            process.inputStream.use { source ->
                source.copyTo(dest) // Copy data from process output to output stream
            }
        }

        // Handle error redirection
        error?.let { dest ->
            process.errorStream.use { source ->
                source.copyTo(dest) // Copy data from process error output to error stream
            }
        }

        val exitCode = process.waitFor() // Wait for the process to complete
        return exitCode
    }

    private fun commandSubtitution(program: AST.Program): String {
        // I need to redirect the output and save it to return it as a string
        val output = StringBuilder()
        val input = "".byteInputStream()
        val subExecutor = Executor(program, input, output.asOutputStream(), error, env)
        subExecutor.execute()
        return output.toString().trim()
    }

    private fun variableSubstitution(variable: AST.VariableSubstitution): String {
        return env[variable.variableName] ?: ""
    }

}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}