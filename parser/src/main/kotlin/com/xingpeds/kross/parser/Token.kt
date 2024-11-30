package com.xingpeds.kross.parser


sealed class Token {
    abstract val type: TokenType

    data class Word(
        val value: String,
    ) : Token() {
        override val type = TokenType.Word
    }
    data object Semicolon : Token() {
        override val type = TokenType.Semicolon
    }

    data object Pipe : Token() {
        override val type = TokenType.Pipe
    }

    data object And : Token() {
        override val type = TokenType.And
    }

    data object Or : Token() {
        override val type = TokenType.Or
    }

    data object LeftParen : Token() {
        override val type = TokenType.LeftParen
    }

    data object RightParen : Token() {
        override val type = TokenType.RightParen
    }
    
}