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
                            AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            listOf(
                                AST.Argument.VariableSubstitution(
                                    "hello",
                                    sourceLocation = 1..1
                                )
                            )
                        ),
                        AST.SimpleCommand(
                            AST.CommandIdentifier(
                                "cat",
                                sourceLocation = 1..1
                            ),
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