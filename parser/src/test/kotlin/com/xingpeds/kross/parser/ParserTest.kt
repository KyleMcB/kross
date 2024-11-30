package com.xingpeds.kross.parser

import kotlin.test.Test

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
}
