package com.xingpeds.kross.parser

import java.io.InputStream
import java.io.OutputStream

class Executor(
    val program: AST.Program,
    val localCommands: Set<String> = emptySet(),
    val environment: Map<String, String> = emptyMap(),
) {
    suspend fun execute() {
        exeProgram(program)
    }

    private suspend fun exeProgram(program: AST.Program) {
        exeSequence(program.statements)
    }

    private suspend fun exeSequence(sequence: AST.Sequence) {
        for (statement in sequence.statements) {
            exeStatement(statement)
        }
    }

    private suspend fun exeStatement(statement: AST.Statement) {
        when (statement) {
            is AST.And -> TODO()
            is AST.Or -> TODO()
            is AST.SimpleCommand -> exeSimpleCommand(statement)
            is AST.Pipeline -> exePipeline(statement)
        }
    }

    private suspend fun exePipeline(statement: AST.Pipeline) {

    }

    private suspend fun exeSimpleCommand(statement: AST.SimpleCommand) {
        when (statement.name) {
            is AST.CommandName.Path -> exeExternalCommand(statement.name.value, statement.arguments)
            is AST.CommandName.Word -> if (localCommands.contains(statement.name.value)) {
                exeLocalCommand(statement.name.value, statement.arguments)
            } else exeExternalCommand(statement.name.value, statement.arguments)

        }
    }

    private suspend fun exeLocalCommand(command: String, args: List<AST.Argument>) {
        TODO()
    }

    private suspend fun exeExternalCommand(
        name: String,
        args: List<AST.Argument>,
        input: InputStream? = null,
        output: OutputStream? = null,
        err: OutputStream? = null
    ): Int {
        // todo I need to get input, output, and err and env in here somehow
        // context reciever is tempting
        val resolvedArgs: List<String> = resolveArguments(args)
        val pb = ProcessBuilder(listOf(name) + resolvedArgs)
        if (input == null) pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        if (output == null) pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        if (err == null) pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        val process = pb.start()
        input?.copyTo(process.outputStream)
        output?.let { process.inputStream.copyTo(it) }
        err?.let { process.errorStream.copyTo(it) }
        val code = process.waitFor()
        process.destroy()
        return code
    }

    private suspend fun resolveArguments(args: List<AST.Argument>): List<String> {
        val resolved = mutableListOf<String>()
        for (arg in args) {
            when (arg) {
                is AST.CommandSubstitution -> TODO()
                is AST.VariableSubstitution -> resolved.add(environment[arg.variableName] ?: "")
                is AST.WordArgument -> resolved.add(arg.value)
            }
        }
        return resolved
    }
}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}