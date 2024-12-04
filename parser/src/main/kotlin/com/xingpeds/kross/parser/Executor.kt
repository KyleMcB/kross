package com.xingpeds.kross.parser

// this probably should not be in the parser module, but I'll deal with that later
class Executor(private val program: AST.Program) {
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
            is AST.Pipeline -> TODO()
            is AST.SimpleCommand -> simpleCommand(statement)
        }
    }

    private fun simpleCommand(command: AST.SimpleCommand) {
        val args = command.arguments.map { argument: AST.Argument ->
            when (argument) {
                is AST.CommandSubstitution -> TODO()
                is AST.VariableSubstitution -> TODO()
                is AST.WordArgument -> argument.value
            }
        }
        val builder = ProcessBuilder(listOf(command.name) + args)
        builder.inheritIO()
        val process = builder.start()
        val exitCode = process.waitFor()
        process.destroy()
        println("exitCode: $exitCode")
    }

}