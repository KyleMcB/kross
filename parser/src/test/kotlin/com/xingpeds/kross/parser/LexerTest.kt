package com.xingpeds.kross.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest {
    @Test
    fun oneWord() = runTest {
        val program = "hello"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(1, tokens.size, "should have one token")
        val expeced = Token.Word("hello")
        assertEquals(expeced, tokens.first(), "should be the expected token")
    }

    @Test
    fun twoWords() = runTest {
        val program = "hello world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(2, tokens.size, "should have two tokens")
    }

    @Test
    fun extraWhiteSpace() = runTest {
        val program = "   hello\n world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(2, tokens.size, "should have two tokens")
    }

    @Test
    fun semicolon() = runTest {
        val program = "hello;world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        println(tokens)
        assertEquals(3, tokens.size, "should have three tokens")
        assertEquals(Token.Word("hello"), tokens[0], "should be the expected token")
        assertEquals(Token.Semicolon, tokens[1], "should be the expected token")
    }

}