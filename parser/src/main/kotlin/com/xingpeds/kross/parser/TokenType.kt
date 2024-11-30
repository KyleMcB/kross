package com.xingpeds.kross.parser

enum class TokenType(
    val matcher: Regex,
    val precedence: Int
) {
    /**
     * always start the regex with a start of string matcher. This will make sure there is one or no matches
     */
    Word(Regex("^[^\\s;()|&]+"), 0),          // Matches any contiguous string of non-whitespace characters at the start
    Semicolon(Regex("^;"), 1),
    Pipe(Regex("^\\|"), 2),
    And(Regex("^&&"), 3),
    Or(Regex("^\\|\\|"), 4),
    LeftParen(Regex("^\\("), 6),
    RightParen(Regex("^\\)"), 6),
}
