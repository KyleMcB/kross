package com.xingpeds.kross.parser

import kotlin.test.Test
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun one() {
        val program = "hello"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
    }

    @Test
    fun two() {
        val program = "hello world"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
    }

    @Test
    fun three() {
        val program = "hello world 2"
        val parser = Parser(program)
        val ast = parser.parse()
        println(ast)
    }

    @Test
    fun testSequenceParsing() {
        val program = "echo hello && echo world"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST is a Sequence with two parts
        assertTrue(ast is AST.Sequence)
    }

    @Test
    fun testAndOperatorParsing() {
        val program = "command1 && command2"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST is a And operator
        assertTrue(ast is AST.And)
    }

    @Test
    fun testOrOperatorParsing() {
        val program = "command1 || command2"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST is a Or operator
        assertTrue(ast is AST.Or)
    }

    @Test
    fun testPipelineParsing() {
        val program = "ls | grep file"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST is a Pipeline
        assertTrue(ast is AST.Pipeline)
    }

    @Test
    fun testSimpleCommandParsing() {
        val program = "echo 'simple command'"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST is a SimpleCommand
        assertTrue(ast is AST.SimpleCommand)
    }

    @Test
    fun testVariableSubstitutionParsing() {
        val program = "echo \$MY_VAR"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST contains a VariableSubstitution
        assertTrue((ast as AST.SimpleCommand).arguments.any { it is AST.VariableSubstitution })
    }

    @Test
    fun testCommandSubstitutionParsing() {
        val program = "echo $(date)"
        val parser = Parser(program)
        val ast = parser.parse()

        println(ast)
        // Check if the AST contains a CommandSubstitution
        assertTrue((ast as AST.SimpleCommand).arguments.any { it is AST.CommandSubstitution })
    }
}
