package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun one() = runTest {
        val program = flowOf(Token.Word("hello", 0..5), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "hello",
                                sourceLocation = 0..5
                            ),
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
        val program = flowOf(Token.Word("hello", 0..5), Token.Word("world", 0..5), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "hello",
                                sourceLocation = 0..5
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "world",
                                    sourceLocation = 0..5
                                )
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
        val program = flowOf(Token.Word("hello", 1..1), Token.Word("world", 1..1), Token.Word("2", 1..1), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "hello",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "world",
                                    sourceLocation = 1..1
                                ),
                                AST.Argument.WordArgument(
                                    "2",
                                    sourceLocation = 1..1
                                )
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
            Token.Word("echo", 1..1),
            Token.Word("hello", 1..1),
            Token.Semicolon(6..6),
            Token.Word("echo", 1..1),
            Token.Word("world", 1..1),
            Token.EOF()
        )
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "hello",
                                    sourceLocation = 1..1
                                )
                            )
                        )
                    )
                ),
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "world",
                                    sourceLocation = 1..1
                                )
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
        val program = flowOf(Token.Word("command1", 1..1), Token.And(1..1), Token.Word("command2", 1..1), Token.EOF())

        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.And(
                    left = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandIdentifier(
                                    "command1",
                                    sourceLocation = 1..1
                                ),
                                arguments = listOf()
                            )
                        )
                    ),
                    right = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandIdentifier(
                                    "command2",
                                    sourceLocation = 1..1
                                ),
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
        val program = flowOf(Token.Word("command1", 1..1), Token.Or(1..1), Token.Word("command2", 1..1), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)
        val expected = AST.Program(
            commands = listOf(
                AST.Command.Or(
                    left = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandIdentifier(
                                    "command1",
                                    sourceLocation = 1..1
                                ),
                                arguments = listOf()
                            )
                        )
                    ),
                    right = AST.Command.Pipeline(
                        commands = listOf(
                            AST.SimpleCommand(
                                name = AST.CommandIdentifier(
                                    "command2",
                                    sourceLocation = 1..1
                                ),
                                arguments = listOf()
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun SinglequoteArguement() = runTest {
        val program = flowOf(Token.Word("echo", 1..1), Token.SingleQuote("hello world", 1..1), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)
        println(ast)
    }

    @Test
    fun doublequoteArguement() = runTest {
        val program = flowOf(Token.Word("echo", 1..1), Token.DoubleQuote("hello world", 1..1), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "hello world",
                                    sourceLocation = 1..1
                                )
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
    fun testPipelineParsing2() = runTest {
//        val program = "ls | grep file | cowsay"
        val program = flowOf(
            Token.Word("ls", 1..1),
            Token.Pipe(1..1),
            Token.Word("grep", 1..1),
            Token.Word("file", 1..1),
            Token.Pipe(1..1),
            Token.Word("cowsay", 1..1),
            Token.EOF()
        )
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "ls",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf()
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "grep",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "file",
                                    sourceLocation = 1..1
                                )
                            )
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "cowsay",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf()
                        )
                    )
                )
            )
        )

        assertEquals(expected, ast)
    }

    //
    @Test
    fun testPipelineParsing() = runTest {
//        val program = "ls | grep file"
        val program = flowOf<Token>(
            Token.Word("ls", 1..1),
            Token.Pipe(1..1),
            Token.Word("grep", 1..1),
            Token.Word("file", 1..1),
            Token.EOF()
        )
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "ls",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf()
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "grep",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.WordArgument(
                                    "file",
                                    sourceLocation = 1..1
                                )
                            )
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

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
//                        AST.CommandIdentifier("echo"),
//                        arguments = listOf(AST.WordArgument("simple"))
//                    )
//                )
//            )
//        )
//        assertEquals(expected, ast)
//    }
//
    @Test
    fun testVariableSubstitutionParsing() = runTest {
//        val program = "echo \$MY_VAR"
        val program = flowOf(Token.Word("echo", 1..1), Token.Dollar(1..1), Token.Word("MY_VAR", 1..1), Token.EOF())
        val parser = Parser()
        val ast = parser.parse(program)

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.VariableSubstitution(
                                    variableName = "MY_VAR",
                                    sourceLocation = 1..1
                                )
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
    fun testCommandSubstitutionParsing() = runTest {
//        val program = "echo (date)"
        val program = flowOf(
            Token.Word("echo", 1..1),
            Token.LeftParen(1..1),
            Token.Word("date", 1..1),
            Token.RightParen(1..1),
            Token.EOF()
        )
        val parser = Parser()

        val expected = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandIdentifier(
                                "echo",
                                sourceLocation = 1..1
                            ),
                            arguments = listOf(
                                AST.Argument.CommandSubstitution(
                                    commandLine = AST.Program(
                                        commands = listOf(
                                            AST.Command.Pipeline(
                                                commands = listOf(
                                                    AST.SimpleCommand(
                                                        name = AST.CommandIdentifier(
                                                            "date",
                                                            sourceLocation = 1..1
                                                        ),
                                                        arguments = listOf()
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    sourceLocation = 1..1
                                )
                            )
                        )
                    )
                )
            )
        )

        val ast = parser.parse(program)
        assertEquals(expected, ast)
    }
}
