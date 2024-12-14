package com.xingpeds.kross.parser


sealed class Token {
    sealed interface Operator

    abstract val type: TokenType

    data object Dollar : Token() {
        override val type = TokenType.Dollar
    }

    sealed class Literal : Token() {
        abstract val value: String
    }

    data class SingleQuote(override val value: String) : Literal() {
        override val type = TokenType.SingleQuotedString
    }

    data class DoubleQuote(override val value: String) : Literal() {
        override val type = TokenType.DoubleQuotedString
    }

    data class Word(
        override val value: String,
    ) : Literal() {
        override val type = TokenType.Word
    }

    data object Semicolon : Token() {
        override val type = TokenType.Semicolon
    }

    data object Pipe : Token() {
        override val type = TokenType.Pipe
    }

    data object And : Token(), Operator {
        override val type = TokenType.And
    }

    data object Or : Token(), Operator {
        override val type = TokenType.Or
    }

    data object LeftParen : Token() {
        override val type = TokenType.LeftParen
    }

    data object RightParen : Token() {
        override val type = TokenType.RightParen
    }

    data object RightBracket : Token() {
        override val type: TokenType = TokenType.RightBracket
    }

    data object LeftBracket : Token() {
        override val type: TokenType = TokenType.LeftBracket
    }

    data object EOF : Token() {

        override val type = TokenType.EOF
    }
}