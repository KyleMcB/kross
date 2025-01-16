package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST
import kotlin.test.Test

class PrettyPrinterTest {
    @Test
    fun manual() {
        val ast = AST.Program(
            listOf(
                AST.Command.Pipeline(
                    listOf(
                        AST.SimpleCommand(
                            AST.CommandName.Word("echo"),
                            listOf(AST.Argument.VariableSubstitution("hello"))
                        ),
                        AST.SimpleCommand(
                            AST.CommandName.Word("cat"),
                        )
                    )
                )
            )
        )
        val pp = PrettyPrinter()
        val output = pp.visitProgram(ast)
        println(output)
    }
}