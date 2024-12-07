package com.xingpeds.kross.parser

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun one() = runTest {
        val program = flowOf(Token.Word("hello"), Token.EOF)
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("hello"),
                            arguments = listOf()
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun two() = runTest {
//        val program = "hello world"
        val program = flowOf(Token.Word("hello"), Token.Word("world"), Token.EOF)
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("hello"),
                            arguments = listOf(
                                AST.Argument.WordArgument("world")
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun three() = runTest {
        val program = flowOf(Token.Word("hello"), Token.Word("world"), Token.Word("2"), Token.EOF)
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("hello"),
                            arguments = listOf(
                                AST.Argument.WordArgument("world"),
                                AST.Argument.WordArgument("2")
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    //
    @Test
    fun testSequenceParsing() = runTest {
//        val program = "echo hello ; echo world"
        val program = flowOf(
            Token.Word("echo"),
            Token.Word("hello"),
            Token.Semicolon,
            Token.Word("echo"),
            Token.Word("world"),
            Token.EOF
        )
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.WordArgument("hello")
                            )
                        )
                    )
                ),
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.WordArgument("world")
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    //
    @Test
    fun testAndOperatorParsing() = runTest {
        val program = flowOf(Token.Word("command1"), Token.And, Token.Word("command2"), Token.EOF)

        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.And(
                    left = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandName.Word("command1"),
                                arguments = listOf()
                            )
                        )
                    ),
                    right = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandName.Word("command2"),
                                arguments = listOf()
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    //
    @Test
    fun testOrOperatorParsing() = runTest {
        val program = flowOf(Token.Word("command1"), Token.Or, Token.Word("command2"), Token.EOF)
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Or(
                    left = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandName.Word("command1"),
                                arguments = listOf()
                            )
                        )
                    ),
                    right = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandName.Word("command2"),
                                arguments = listOf()
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

//    @Test
//    fun SinglequoteArguement() {
//        val program = "echo \'hello world\'"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.SimpleCommand(
//                        AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("'hello world'"))
//                    ),
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun doublequoteArguement() {
//        val program = "echo \"hello world\""
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.SimpleCommand(
//                        AST.CommandName.Word("echo"), arguments = listOf(AST.WordArgument("\"hello world\""))
//                    ),
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun testPipelineParsing2() {
//        val program = "ls | grep file | cowsay"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.Pipeline(
//                        listOf(
//                            AST.SimpleCommand(AST.CommandName.Word("ls")),
//                            AST.SimpleCommand(
//                                AST.CommandName.Word("grep"),
//                                arguments = listOf(AST.WordArgument("file"))
//                            ),
//                            AST.SimpleCommand(AST.CommandName.Word("cowsay"))
//                        )
//                    )
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun testPipelineParsing() {
//        val program = "ls | grep file"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.Pipeline(
//                        listOf(
//                            AST.SimpleCommand(AST.CommandName.Word("ls")),
//                            AST.SimpleCommand(
//                                AST.CommandName.Word("grep"),
//                                arguments = listOf(AST.WordArgument("file"))
//                            ),
//                        )
//                    )
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun testSimpleCommandParsing() {
//        val program = "echo simple"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.SimpleCommand(
//                        AST.CommandName.Word("echo"),
//                        arguments = listOf(AST.WordArgument("simple"))
//                    )
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun testVariableSubstitutionParsing() {
//        val program = "echo \$MY_VAR"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        // Check if the AST contains a VariableSubstitution
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.SimpleCommand(
//                        AST.CommandName.Word("echo"), arguments = listOf(
//                            AST.VariableSubstitution("MY_VAR")
//                        )
//                    )
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
//    @Test
//    fun testCommandSubstitutionParsing() {
//        val program = "echo (date)"
//        val parser = Parser(program)
//        val ast = parser.parse()
//        val expected = AST.Program(
//            AST.Sequence(
//                listOf(
//                    AST.SimpleCommand(
//                        AST.CommandName.Word("echo"), arguments = listOf(
//                            AST.CommandSubstitution(
//                                AST.Program(AST.Sequence(listOf(AST.SimpleCommand(AST.CommandName.Word("date"))))),
//                            )
//                        )
//                    ),
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
}
