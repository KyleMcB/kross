package com.xingpeds.kross.parser

import java.io.OutputStream

class Executor(val program: AST.Program) {
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
            is AST.Pipeline -> TODO()
        }
    }

    private suspend fun exeSimpleCommand(statement: AST.SimpleCommand) {
//       val
    }
}

fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        append(b.toChar())
    }
}