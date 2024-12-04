package com.xingpeds.kross.parser

import kotlin.test.Test

class ExecutorTest {
    @Test
    fun simpleEcho() {
        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.WordArgument("hello world")))))
        val executor = Executor(ast)
        executor.execute()
    }
}