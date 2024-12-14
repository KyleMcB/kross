package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private val whiteSpaceMatcher = Regex("^\\s+")

class Lexer(
    private val input: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var cursor = 0

    fun tokens(): Flow<Token> = flow {
        while (cursor <= input.lastIndex) {
            val string = input.substring(cursor)

            // Skip whitespace
            val ws = whiteSpaceMatcher.find(string)
            if (ws != null) {
                cursor += ws.value.length
                continue
            }

            // Match tokens in parallel
            val match = TokenType.entries.parallelMap(coroutineScope) { tokenType ->
                val match = tokenType.matcher.find(string)
                match?.let { it to tokenType }
            }.filterNotNull()
                .maxByOrNull { it.second.precedence } // Choose the highest precedence match

            if (match != null) {
                val (matchResult, tokenType) = match
                val tokenText = matchResult.value
                val token = createTokenFrom(tokenText, tokenType)
                cursor += tokenText.length
                emit(token)
            } else {
                val snippet = string.take(10)
                throw Exception("Unexpected character '${string[0]}' at cursor $cursor near: \"$snippet\"")
            }
        }
        emit(Token.EOF)
    }

    private fun createTokenFrom(text: String, tokenType: TokenType): Token {
        return when (tokenType) {
            TokenType.Word -> Token.Word(text)
            TokenType.Semicolon -> Token.Semicolon
            TokenType.Pipe -> Token.Pipe
            TokenType.And -> Token.And
            TokenType.Or -> Token.Or
            TokenType.LeftParen -> Token.LeftParen
            TokenType.RightParen -> Token.RightParen
            TokenType.Dollar -> Token.Dollar
            TokenType.EOF -> Token.EOF
            TokenType.SingleQuotedString -> Token.SingleQuote(text)
            TokenType.DoubleQuotedString -> Token.DoubleQuote(text)
            TokenType.LeftBracket -> Token.LeftBracket
            TokenType.RightBracket -> Token.RightBracket
        }
    }
}

suspend fun <A, B> Iterable<A>.parallelMap(
    scope: CoroutineScope,
    transform: suspend (A) -> B
): List<B> = scope.run {
    map { element ->
        async { transform(element) }
    }.awaitAll()
}
