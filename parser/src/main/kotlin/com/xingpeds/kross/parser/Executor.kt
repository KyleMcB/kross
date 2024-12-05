package com.xingpeds.kross.parser

import java.io.InputStream
import java.io.OutputStream

// this probably should not be in the parser module, but I'll deal with that later
class Executor(
    private val program: AST.Program,
//    private val stdin: InputStream = InputStream.JavaPipe(stream = ProcessBuilder.Redirect.INHERIT),
//    private val stdout: OutStream = OutStream.JavaPipe(stream = ProcessBuilder.Redirect.INHERIT),
//    private val stderr: OutStream.JavaPipe = OutStream.JavaPipe(stream = ProcessBuilder.Redirect.INHERIT),
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

    fun executeWithRedirect(
        command: List<String>,
        input: InputStream? = null,  // Stream to provide input to the process
        output: OutputStream? = null // Stream to capture the process's output
    ) {
        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to PIPE
            .redirectInput(ProcessBuilder.Redirect.PIPE)  // Redirect stdin to PIPE
            .start()

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

        process.waitFor() // Wait for the process to finish
    }

    fun simpleCommand(
        command: AST.SimpleCommand,
        input: InputStream? = null,  // Stream to provide input to the process
        output: OutputStream? = null, // Stream to capture the process's output
        error: InputStream? = null,
    ) {
        val args = command.arguments.map { argument: AST.Argument ->
            when (argument) {
                is AST.CommandSubstitution -> TODO()
                is AST.VariableSubstitution -> TODO()
                is AST.WordArgument -> argument.value
            }
        }
        val builder = ProcessBuilder(listOf(command.name) + args)
        if (input == null) {
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        if (output == null) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }

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
        val exitCode = process.waitFor()
        println("exitCode: $exitCode")
    }

    private fun commandSubtitution(program: AST.Program): String {
        // I need to redirect the output and save it to return it as a string
//        val outputFile = File()
//        val savedOutput = ProcessBuilder.Redirect.to(outputFile)
//        val subExecutor = Executor(program, input, savedOutput, error, env)
        return ""
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