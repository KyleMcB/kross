package com.xingpeds.kross.parser


sealed class Token {
    abstract val sourcePosition: IntRange

    sealed interface Operator

    abstract val type: TokenType

    data class Dollar(override val sourcePosition: IntRange) : Token() {
        override val type = TokenType.Dollar
    }

    sealed class Literal : Token() {
        abstract val value: String
    }

    data class SingleQuote(override val value: String, override val sourcePosition: IntRange) : Literal() {
        override val type = TokenType.SingleQuotedString
    }

    data class DoubleQuote(override val value: String, override val sourcePosition: IntRange) : Literal() {
        override val type = TokenType.DoubleQuotedString
    }

    data class Word(
        val rawValue: String,
        override val sourcePosition: IntRange,
        override val value: String = rawValue.replace("\\ ", " ") // Replace escaped spaces
            .replace("\\;", ";") // Replace escaped semicolon
            .replace("\\|", "|") // Replace escaped pipe
            .replace("\\&", "&") // Replace escaped ampersand
            .replace("\\(", "(") // Replace escaped left parenthesis
            .replace("\\)", ")") // Replace escaped right parenthesis,
    ) : Literal() {
        override val type = TokenType.Word
    }

    data class Semicolon(override val sourcePosition: IntRange) : Token() {
        override val type = TokenType.Semicolon
    }

    data class Pipe(override val sourcePosition: IntRange) : Token() {
        override val type = TokenType.Pipe
    }

    data class And(override val sourcePosition: IntRange) : Token(), Operator {
        override val type = TokenType.And
    }

    data class Or(override val sourcePosition: IntRange) : Token(), Operator {
        override val type = TokenType.Or
    }

    data class LeftParen(override val sourcePosition: IntRange) : Token() {
        override val type = TokenType.LeftParen
    }

    data class RightParen(override val sourcePosition: IntRange) : Token() {
        override val type = TokenType.RightParen
    }

    data class EOF(override val sourcePosition: IntRange = 0..0) : Token() {

        override val type = TokenType.EOF
    }

    data class DoubleQuoteWithVar(val text: String, override val sourcePosition: IntRange) : Token() {

        override val type: TokenType
            get() = TokenType.DoubleQuotedStringWithEnv
    }

    data class Glob(val text: String, override val sourcePosition: IntRange) : Token() {
        override val type: TokenType
            get() = TokenType.WordWithGlob
    }

    data class RecursiveGlob(val text: String, override val sourcePosition: IntRange) : Token() {
        override val type: TokenType
            get() = TokenType.WordWithDoubleGlob
    }
}