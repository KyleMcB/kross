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
                val token = createTokenFrom(tokenText, tokenType, cursor)
                cursor += tokenText.length
                emit(token)
            } else {
                val snippet = string.take(10)
                throw Exception("Unexpected character '${string[0]}' at cursor $cursor near: \"$snippet\"")
            }
        }
        emit(Token.EOF(cursor - 1..cursor - 1))
    }

    private fun createTokenFrom(text: String, tokenType: TokenType, atPosition: Int): Token {
        return when (tokenType) {
            TokenType.Word -> Token.Word(text, (atPosition..text.length + atPosition - 1))
            TokenType.Semicolon -> Token.Semicolon(atPosition..atPosition)
            TokenType.Pipe -> Token.Pipe(atPosition..atPosition)
            TokenType.And -> Token.And(atPosition..atPosition + 1)
            TokenType.Or -> Token.Or(atPosition..atPosition + 1)
            TokenType.LeftParen -> Token.LeftParen(atPosition..atPosition)
            TokenType.RightParen -> Token.RightParen(atPosition..atPosition)
            TokenType.Dollar -> Token.Dollar(atPosition..atPosition)
            TokenType.EOF -> Token.EOF(atPosition..atPosition)
            TokenType.SingleQuotedString -> Token.SingleQuote(text, (atPosition..text.length + atPosition - 1))
            TokenType.DoubleQuotedString -> Token.DoubleQuote(text, (atPosition..text.length + atPosition - 1))
            TokenType.LeftBracket -> Token.LeftBracket(atPosition..atPosition)
            TokenType.RightBracket -> Token.RightBracket(atPosition..atPosition)
            TokenType.DoubleQuotedStringWithEnv -> Token.DoubleQuoteWithVar(
                text,
                (atPosition..text.length + atPosition - 1)
            )
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
