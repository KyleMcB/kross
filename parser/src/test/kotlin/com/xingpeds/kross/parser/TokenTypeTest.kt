package com.xingpeds.kross.parser

import kotlin.test.Test

class TokenTypeTest {

    @Test
    fun doubleStar() {
        val program = "hello**.txt"
        val match = TokenType.WordWithDoubleGlob.matcher.find(program)
        println(match)
    }
}