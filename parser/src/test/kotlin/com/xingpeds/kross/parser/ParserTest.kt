package com.xingpeds.kross.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun one() {
        val program = "hello"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(listOf(AST.SimpleCommand(AST.CommandName.Word("hello"))))
        )
        assertEquals(expected, ast)
    }

    @Test
    fun two() {
        val program = "hello world"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("hello"),
                        arguments = listOf(AST.WordArgument("world"))
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun three() {
        val program = "hello world 2"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                statements = listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("hello"),
                        arguments = listOf(AST.WordArgument("world"), AST.WordArgument("2"))
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testSequenceParsing() {
        val program = "echo hello ; echo world"
        val parser = Parser(program)
        val ast = parser.parse()

        val expected = AST.Program(
            statements = AST.Sequence(
                statements = listOf(
                    AST.SimpleCommand(AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("hello"))),
                    AST.SimpleCommand(AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("world"))),
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testAndOperatorParsing() {
        val program = "command1 && command2"
        val parser = Parser(program)
        val ast = parser.parse()

        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.And(
                        AST.SimpleCommand(AST.CommandName.Word("command1")),
                        AST.SimpleCommand(AST.CommandName.Word("command2"))
                    ),
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testOrOperatorParsing() {
        val program = "command1 || command2"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.Or(
                        AST.SimpleCommand(
                            AST.CommandName.Word("command1")
                        ), AST.SimpleCommand(
                            AST.CommandName.Word("command2")
                        )
                    ),
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun SinglequoteArguement() {
        val program = "echo \'hello world\'"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("'hello world'"))
                    ),
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun doublequoteArguement() {
        val program = "echo \"hello world\""
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("\"hello world\""))
                    ),
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testPipelineParsing2() {
        val program = "ls | grep file | cowsay"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.Pipeline(
                        listOf(
                            AST.SimpleCommand(AST.CommandName.Word("ls")),
                            AST.SimpleCommand(
                                AST.CommandName.Word("grep"),
                                arguments = listOf(AST.WordArgument("file"))
                            ),
                            AST.SimpleCommand(AST.CommandName.Word("cowsay"))
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testPipelineParsing() {
        val program = "ls | grep file"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.Pipeline(
                        listOf(
                            AST.SimpleCommand(AST.CommandName.Word("ls")),
                            AST.SimpleCommand(
                                AST.CommandName.Word("grep"),
                                arguments = listOf(AST.WordArgument("file"))
                            ),
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testSimpleCommandParsing() {
        val program = "echo simple"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("echo"),
                        arguments = listOf(AST.WordArgument("simple"))
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testVariableSubstitutionParsing() {
        val program = "echo \$MY_VAR"
        val parser = Parser(program)
        val ast = parser.parse()
        // Check if the AST contains a VariableSubstitution
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("echo"), arguments = listOf(
                            AST.VariableSubstitution("MY_VAR")
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testCommandSubstitutionParsing() {
        val program = "echo (date)"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            AST.Sequence(
                listOf(
                    AST.SimpleCommand(
                        AST.CommandName.Word("echo"), arguments = listOf(
                            AST.CommandSubstitution(
                                AST.Program(AST.Sequence(listOf(AST.SimpleCommand(AST.CommandName.Word("date"))))),
                            )
                        )
                    ),
                )
            )
        )
        assertEquals(expected, ast)
    }
}
