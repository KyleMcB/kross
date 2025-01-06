package com.xingpeds.kross.parser


sealed class Token {
    abstract val position: IntRange

    sealed interface Operator

    abstract val type: TokenType

    data class Dollar(override val position: IntRange) : Token() {
        override val type = TokenType.Dollar
    }

    sealed class Literal : Token() {
        abstract val value: String
    }

    data class SingleQuote(override val value: String, override val position: IntRange) : Literal() {
        override val type = TokenType.SingleQuotedString
    }

    data class DoubleQuote(override val value: String, override val position: IntRange) : Literal() {
        override val type = TokenType.DoubleQuotedString
    }

    data class Word(
        override val value: String, override val position: IntRange,
    ) : Literal() {
        override val type = TokenType.Word
    }

    data class Semicolon(override val position: IntRange) : Token() {
        override val type = TokenType.Semicolon
    }

    data class Pipe(override val position: IntRange) : Token() {
        override val type = TokenType.Pipe
    }

    data class And(override val position: IntRange) : Token(), Operator {
        override val type = TokenType.And
    }

    data class Or(override val position: IntRange) : Token(), Operator {
        override val type = TokenType.Or
    }

    data class LeftParen(override val position: IntRange) : Token() {
        override val type = TokenType.LeftParen
    }

    data class RightParen(override val position: IntRange) : Token() {
        override val type = TokenType.RightParen
    }

    data class RightBracket(override val position: IntRange) : Token() {
        override val type: TokenType = TokenType.RightBracket
    }

    data class LeftBracket(override val position: IntRange) : Token() {
        override val type: TokenType = TokenType.LeftBracket
    }

    data class EOF(override val position: IntRange = 0..0) : Token() {

        override val type = TokenType.EOF
    }
}