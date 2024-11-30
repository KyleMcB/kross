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
            val ws = whiteSpaceMatcher.find(string)
            if (ws != null) {
                cursor += ws.value.length
                continue
            }
            val match: Pair<MatchResult, TokenType>? = TokenType.entries.parallelMap(coroutineScope) { tokenType ->
                val match = tokenType.matcher.find(string)
                return@parallelMap if (match != null) {
                    match to tokenType
                } else {
                    null
                }
            }.filterNotNull().sortedBy { it.second.precedence }.reversed().firstOrNull()
            if (match != null) {
                //
                val token = createTokenFrom(string.substring(0, match.first.value.length), match.second)
                cursor += match.first.value.length
                emit(token)
            } else {
                throw Exception("Unexpected character at $cursor\n\"$string\"")
            }

        }
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
        }
    }
}

suspend fun <A, B> Iterable<A>.parallelMap(
    scope: CoroutineScope,
    transform: suspend (A) -> B
): List<B> = scope.run {
    map { element ->
        async { transform(element) }
    }.awaitAll() // Collect all the deferred results
}