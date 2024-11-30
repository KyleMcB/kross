package com.xingpeds.kross.parser


sealed class Token {
    abstract val type: TokenType

    data class Word(
        val value: String,
    ) : Token() {
        override val type = TokenType.Word
    }
}