package com.xingpeds.kross

import com.xingpeds.kross.parser.Lexer
import com.xingpeds.kross.parser.Parser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class HighlighterTest {
    @Test
    fun envVar() = runTest {
        val program = "echo \$hello"
        val lexer = Lexer(program)
        val ast = Parser().parse(lexer.tokens())
        println(ast)
        val highlighter = Highlighter() { TextStyle.Command.Valid }
        val list = highlighter.visitProgram(ast)
        println(list)
    }

    @Test
    fun commSub() = runTest {
        val program = "echo (hello )"
        val lexer = Lexer(program)
        val ast = Parser().parse(lexer.tokens())
        println(ast)
        val highlighter = Highlighter() { TextStyle.Command.Valid }
        val list = highlighter.visitProgram(ast)
        println(list)
    }
}