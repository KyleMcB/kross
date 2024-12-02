package com.xingpeds.kross.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun one() {
        val program = "hello"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(listOf(AST.SimpleCommand("hello")))
        assertEquals(expected, ast)
    }

    @Test
    fun two() {
        val program = "hello world"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(listOf(AST.SimpleCommand("hello", listOf(AST.WordArgument("world")))))
        assertEquals(expected, ast)
    }

    @Test
    fun three() {
        val program = "hello world 2"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected =
            AST.Program(listOf(AST.SimpleCommand("hello", listOf(AST.WordArgument("world"), AST.WordArgument("2")))))
        assertEquals(expected, ast)
    }

    @Test
    fun testSequenceParsing() {
        val program = "echo hello ; echo world"
        val parser = Parser(program)
        val ast = parser.parse()

        val expected = AST.Program(
            listOf(
                AST.SimpleCommand("echo", listOf(AST.WordArgument("hello"))),
                AST.SimpleCommand("echo", listOf(AST.WordArgument("world")))
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun testAndOperatorParsing() {
        val program = "command1 && command2"
        val parser = Parser(program)
        val ast = parser.parse()

        val expected = AST.Program(listOf(AST.And(AST.SimpleCommand("command1"), AST.SimpleCommand("command2"))))
        assertEquals(expected, ast)
    }

    @Test
    fun testOrOperatorParsing() {
        val program = "command1 || command2"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        val expected = AST.Program(listOf(AST.Or(AST.SimpleCommand("command1"), AST.SimpleCommand("command2"))))
        assertEquals(expected, ast)
    }

    @Test
    fun testPipelineParsing() {
        val program = "ls | grep file"
        val parser = Parser(program)
        val ast = parser.parse()
        val expected = AST.Program(
            listOf(
                AST.Pipeline(
                    listOf(
                        AST.SimpleCommand("ls"),
                        AST.SimpleCommand("grep", listOf(AST.WordArgument("file")))
                    )
                )
            )
        )
        assertEquals(expected, ast)
        // Check if the AST is a Pipeline
    }

    @Test
    fun testSimpleCommandParsing() {
        // FIXME: need to add quote parsing
        val program = "echo 'simple command'"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
        // Check if the AST is a SimpleCommand
    }

    @Test
    fun testVariableSubstitutionParsing() {
        val program = "echo \$MY_VAR"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
        // Check if the AST contains a VariableSubstitution
    }

    @Test
    fun testCommandSubstitutionParsing() {
        val program = "echo (date)"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
        // Check if the AST contains a CommandSubstitution
    }
}
