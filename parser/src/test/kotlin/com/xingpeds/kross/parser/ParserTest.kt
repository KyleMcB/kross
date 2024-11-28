package com.xingpeds.kross.parser
import kotlin.test.Test

// Represents different types of tokens that can be parsed from the input
sealed class Token  {
    data object Pipe : Token() // Token for '|'

    data object Semicolon : Token() // Token for ';'

    data object And : Token() // Token for '&&'

    data object Or : Token() // Token for '||'

    data class Word(
        val text: String,
    ) : Token() // Token for any word (command or argument)

    data object Dollar : Token() // Token for '$'

    data object OpenParen : Token() // Token for '('

    data object CloseParen : Token() // Token for ')'

    data object OpenBrace : Token() // Token for '{'

    data object CloseBrace : Token() // Token for '}'

    data object EOF : Token() // Token indicating end of input
}

// Enum representing different types of syntax that can be tokens
// Each token type has a corresponding symbol
enum class TokenSyntax(
    val symbol: String,
) {
    PIPE("|"),
    SEMICOLON(";"),
    AND("&&"),
    OR("||"),
    DOLLAR("$"),
    OPEN_PAREN("("),
    CLOSE_PAREN(")"),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}"),
    ;

    companion object {
        // Create a map of symbols to their corresponding TokenSyntax for easy lookup
        private val symbolMap = values().associateBy { it.symbol }

        // Function to get a TokenSyntax from a given symbol string
        fun fromSymbol(symbol: String): TokenSyntax? = symbolMap[symbol]
    }
}

// Lexer function to convert an input string into a list of tokens
fun lexer(input: String): List<Token> {
    println("Starting lexer with input: \"$input\"")
    // Split the input string into words, using whitespace as the delimiter
    val wordList = input.split(Regex("\\s+"))
    println("Split input into words: $wordList")

    // Map each word to a Token based on its value
    val tokens =
        wordList.map { string ->
            // Try to match the string to a predefined TokenSyntax
            val token =
                when (val tokenSyntax = TokenSyntax.fromSymbol(string)) {
                    TokenSyntax.PIPE -> Token.Pipe
                    TokenSyntax.SEMICOLON -> Token.Semicolon
                    TokenSyntax.AND -> Token.And
                    TokenSyntax.OR -> Token.Or
                    TokenSyntax.DOLLAR -> Token.Dollar
                    TokenSyntax.OPEN_PAREN -> Token.OpenParen
                    TokenSyntax.CLOSE_PAREN -> Token.CloseParen
                    TokenSyntax.OPEN_BRACE -> Token.OpenBrace
                    TokenSyntax.CLOSE_BRACE -> Token.CloseBrace
                    // If the string does not match any predefined syntax, it is treated as a word token
                    null -> Token.Word(string)
                }
            println("Mapped word \"$string\" to token: $token")
            token
        }

    println("Finished tokenizing input. Tokens: $tokens")
    return tokens + Token.EOF // Add an EOF token at the end of the list
}

class ParserTest {
    @Test
    fun one() {
        println("hello")
        println(lexer("hello"))
    }
}
