//package com.xingpeds.kross.parser
//
//import java.io.InputStream
//import java.io.OutputStream
//
//// this probably should not be in the parser module, but I'll deal with that later
//class Executor(
//    private val program: AST.Program,
//    private val context: Context = Context()
//) {
//    data class Context(
//        val stdin: InputStream = System.`in`,  // Standard input by default
//        val stdout: OutputStream = System.out, // Standard output by default
//        val stderr: OutputStream = System.err,  // Standard error by default
//        val environment: Map<String, String> = System.getenv() // Default environment
//    )
//
//    fun execute() {
//        executeProgram(program)
//    }
//
//    private fun executeProgram(program: AST.Program) {
//        for (statement in program.statements) {
//            exeStatment(statement)
//        }
//    }
//
//    private fun subCommand(subCommand: AST.BinaryCommand): Int {
//        return when (subCommand) {
//            is AST.And -> and(subCommand)
//            is AST.Or -> or(subCommand)
//            is AST.SimpleCommand -> simpleCommand(subCommand)
//        }
//    }
//
//    private fun exeStatment(statement: AST.Command): Int {
//        return when (statement) {
//            is AST.And -> and(statement)
//            is AST.Or -> or(statement)
//            is AST.Pipeline -> pipeline(statement)
//            is AST.SimpleCommand -> simpleCommand(statement)
//        }
//    }
//
//    private fun and(command: AST.And): Int {
//        val code = simpleCommand(command = command.left)
//        if (code == 0) {
//            val otherCode = subCommand(command.right)
//            return code + otherCode
//        }
//        return code
//    }
//
//    private fun or(command: AST.Or): Int {
//        val code = simpleCommand(command = command.left)
//        if (code != 0) {
//            val othercode = subCommand(command.right)
//            return code * othercode
//        }
//        return 0
//    }
//
//    private fun pipeline(pipeline: AST.Pipeline): Int {
//        val commandList = pipeline.commands
//        commandList.forEachIndexed { index, command ->
//            when (index) {
//                0 -> {
//                    // first one inherits our input
//                }
//
//                commandList.lastIndex -> {
//                    // last one gets out output
//                }
//
//                else -> {
//                    // middle ones get input and output redirected
//                }
//            }
//        }
//        return 0
//    }
//
//    fun simpleCommand(
//        command: AST.SimpleCommand,
//        context: Context = this.context
//    ): Int {
//        val input = context.stdin
//        val output = context.stdout
//        val err = context.stderr
//        val env = context.environment
//        val args = command.arguments.map { argument: AST.Argument ->
//            when (argument) {
//                is AST.CommandSubstitution -> commandSubtitution(argument.commandLine)
//                is AST.VariableSubstitution -> variableSubstitution(argument)
//                is AST.WordArgument -> argument.value
//            }
//        }
//        val builder = ProcessBuilder(listOf(command.name) + args)
//        val pbenv: MutableMap<String, String> = builder.environment()
//
//        pbenv.putAll(env)
//
//        val process = builder.start()
//
//        // Handle input redirection
//        input.let { source ->
//            process.outputStream.use { dest ->
//                source.copyTo(dest) // Copy data from input stream to process input
//            }
//        }
//
//        // Handle output redirection
//        output.let { dest ->
//            process.inputStream.use { source ->
//                source.copyTo(dest) // Copy data from process output to output stream
//            }
//        }
//
//        // Handle error redirection
//        err.let { dest ->
//            process.errorStream.use { source ->
//                source.copyTo(dest) // Copy data from process error output to error stream
//            }
//        }
//
//        val exitCode = process.waitFor() // Wait for the process to complete
//        return exitCode
//    }
//
//    private fun commandSubtitution(program: AST.Program): String {
//        // I need to redirect the output and save it to return it as a string
//        val output = StringBuilder()
//        val input = "".byteInputStream()
//        val context = Context(
//            stdin = input,
//            stdout = output.asOutputStream(),
//            stderr = this.context.stderr,
//            environment = this.context.environment
//        )
//        val subExecutor = Executor(program, context)
//        subExecutor.execute()
//        return output.toString().trim()
//    }
//
//    private fun variableSubstitution(variable: AST.VariableSubstitution): String {
//        return context.environment[variable.variableName] ?: ""
//    }
//
//}
//
//fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
//    override fun write(b: Int) {
//        append(b.toChar())
//    }
//}