package com.xingpeds.kross.parser

val specialCharacters = listOf(';', '|', '&', '(', ')')

private fun specialCharactersToRegex(): String {
    return specialCharacters.joinToString("") { "\\$it" }
}

private val sc: String = specialCharactersToRegex()

interface Matcher {
    fun find(text: String): String?
}

data class RegexMatcher(val pattern: String, val regex: Regex = Regex(pattern)) : Matcher {
    override fun find(text: String): String? {
        return regex.find(text)?.value
    }
}

object TwoStarMatcher : Matcher {
    override fun find(text: String): String? {
        // get the first word out of text
        val word = text.split("\\s+".toRegex()).firstOrNull()
        return if (word?.count { it == '*' } ?: 0 > 1) word else null
    }
}

enum class TokenType(
    val matcher: Matcher,
    val precedence: Int
) {
    /**
     * always start the regex with a start of string matcher. This will make sure there is one or no matches
     */
    Word(
        RegexMatcher("^(?:\\\\.|[^\\s${specialCharactersToRegex()}])+"),
        0
    ),          // Matches any contiguous string of non-whitespace characters at the start

    WordWithGlob(
        // 1) Lookahead `(?=.*\\*)` asserts there's at least one asterisk
        // 2) `^(?:\\.|[^\\s${sc}])+$` ensures the entire token is valid "Word" chars
        RegexMatcher("^(?=.*\\*)(?:\\\\.|[^\\s${specialCharactersToRegex()}])+$"),
        1
    ),
    WordWithDoubleGlob(
        TwoStarMatcher,
        2
    ),
    Semicolon(RegexMatcher("^;"), 1),
    Pipe(RegexMatcher("^\\|"), 2),
    And(RegexMatcher("^&&"), 3),
    Or(RegexMatcher("^\\|\\|"), 4),
    LeftParen(RegexMatcher("^\\("), 6),
    RightParen(RegexMatcher("^\\)"), 6),
    SingleQuotedString(
        RegexMatcher("^'([^'\\\\]|\\\\.)*'"),
        precedence = 7
    ),  // Handles escaped characters within single quotes
    DoubleQuotedString(
        RegexMatcher("^\"([^\"\\\\]|\\\\.)*\""),
        precedence = 7
    ), // Handles escaped characters within double quotes
    Dollar(RegexMatcher("^\\$"), 7),
    DoubleQuotedStringWithEnv(
        RegexMatcher("^\".*(?<!\\\\)\\$.*\""),
        precedence = 8
    ),
    EOF(RegexMatcher("^$"), 8);
}