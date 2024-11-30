package com.xingpeds.kross.parser

enum class TokenType(
    val matcher: Regex,
    val precedence: Int
) {

    Word(Regex("^\\S+"), 0),          // Matches any contiguous string of non-whitespace characters at the start
}