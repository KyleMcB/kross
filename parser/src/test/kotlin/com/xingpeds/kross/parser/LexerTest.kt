package com.xingpeds.kross.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest {
    @Test
    fun oneWord() {
        val program = "hello"
        val lexer = Lexer(program)
        val tokens = lexer.tokens()
        assertEquals(1, tokens.size, "should have one token")
        val expeced = Token.Word("hello")
        assertEquals(expeced, tokens.first(), "should be the expected token")
    }

    @Test
    fun twoWords() {
        val program = "hello world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens()
        assertEquals(2, tokens.size, "should have two tokens")
    }

    @Test
    fun extraWhiteSpace() {
        val program = "   hello\n world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens()
        assertEquals(2, tokens.size, "should have two tokens")
    }
}