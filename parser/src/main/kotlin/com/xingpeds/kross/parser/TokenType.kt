package com.xingpeds.kross.parser

val specialCharacters = listOf(';', '|', '&', '(', ')')

private fun specialCharactersToRegex(): String {
    return specialCharacters.joinToString("") { "\\$it" }
}

private val sc: String = specialCharactersToRegex()

enum class TokenType(
    val matcher: Regex,
    val precedence: Int
) {
    /**
     * always start the regex with a start of string matcher. This will make sure there is one or no matches
     */
    Word(
        Regex("^[^\\s$sc]+"),
        0
    ),          // Matches any contiguous string of non-whitespace characters at the start
    Semicolon(Regex("^;"), 1),
    Pipe(Regex("^\\|"), 2),
    And(Regex("^&&"), 3),
    Or(Regex("^\\|\\|"), 4),
    LeftParen(Regex("^\\("), 6),
    RightParen(Regex("^\\)"), 6),
    Path(Regex("^/([^\\s$sc]*)"), 5),
    Dollar(Regex("^\\$"), 7),
    EOF(Regex("^$"), 8);
}
