package com.xingpeds.kross.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class LexerTest {
    @Test
    fun oneWord() = runTest {
        val program = "hello"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(2, tokens.size, "should have one token")
        val expeced = Token.Word("hello", 0..4)
        assertEquals(expeced, tokens.first(), "should be the expected token")
    }

    @Test
    fun twoWords() = runTest {
        val program = "hello world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        println(tokens)
        println(program.length)
        tokens.forEach { token ->
            println(program.substring(token.position))
        }
        assertEquals(3, tokens.size, "should have two tokens")
    }

    @Test
    fun extraWhiteSpace() = runTest {
        val program = "   hello\n world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(3, tokens.size, "should have two tokens")
    }

    @Test
    fun semicolon() = runTest {
        val program = "hello;world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        println(tokens)
        assertEquals(4, tokens.size, "should have three tokens")
        assertEquals(Token.Word("hello", 0..4), tokens[0], "should be the expected token")
        assertEquals(Token.Semicolon(5..5), tokens[1], "should be the expected token")
    }

    @Test
    fun variableSubstitutionSimple() = runTest {
        val program = "hello \$world"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertIs<Token.Word>(tokens[0])
        assertIs<Token.Dollar>(tokens[1])
        assertIs<Token.Word>(tokens[2])

    }

    @Test
    fun variableSubstitution() = runTest {
        val program = """
            "hello ${'$'}world"
        """.trimIndent()
        println(program)
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(2, tokens.size, "should have two tokens")
        assertIs<Token.DoubleQuoteWithVar>(tokens[0])
    }

    @Test
    fun variableSubstitutionBraces() = runTest {
        val program = """
            "hello ${'$'}{world}"
        """.trimIndent()
        println(program)
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()

        assertEquals(2, tokens.size, "should have two tokens")
        assertIs<Token.DoubleQuoteWithVar>(tokens[0])
    }

    @Test
    fun doublequotewithvarregextest() {
        val oneTrue = """
        "hello ${'$'}world"
    """.trimIndent()
        val matcher = Regex("^\".*(?<!\\\\)\\$.*\"")
        val matchOne = matcher.find(oneTrue)
        assertNotNull(matchOne?.value)
        val twoFalse = """
            "hello \${'$'}world"
        """.trimIndent()
        val matchTwo = matcher.find(twoFalse)
        assertNull(matchTwo?.value, twoFalse)

    }

    @Test
    fun glob() = runTest {
        val program = "*.txt"
        val lexer = Lexer(program)
        val tokens = lexer.tokens().toList()
        assertEquals(2, tokens.size, "should have one token")
        assertEquals(Token.Glob("*.txt", 0..4), tokens[0], "should be the expected glob token")
    }
}