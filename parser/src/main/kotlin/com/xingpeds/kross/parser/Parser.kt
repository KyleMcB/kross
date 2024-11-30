package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Parser.kt
/**
input          ::= sequence

sequence       ::= pipeline { operator pipeline }

operator       ::= '&&' | '||' | ';'

pipeline       ::= command { '|' command }

command        ::= WORD { argument }

argument       ::= WORD | substitution

substitution   ::= variable_substitution | command_substitution

variable_substitution ::= '$' '{'? WORD '}'?

command_substitution ::= '`' command_line '`' | '$(' command_line ')'

command_line   ::= input
 */
class Parser(
    private val input: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : CoroutineScope by coroutineScope {
    private val lexer = Lexer(input, coroutineScope)
    private val tokens = lexer.tokens()
    private lateinit var lookahead: Token

    init {
        runBlocking {
            lookahead = tokens.first()
        }
    }

    fun parse(): AST {
        return parseCommand()
    }

    private fun parseCommand(): AST.Command {
        return parseSimpleCommand()
    }

    private fun eat(vararg types: TokenType): Token = eat(types.toList())
    private fun eat(expectedTypes: List<TokenType>): Token {
        val token = lookahead
        if (expectedTypes.contains(token.type)) {
            runBlocking {
                lookahead = tokens.first()
            }
            return token
        } else {
            val types = expectedTypes.joinToString(", ")
            throw SyntaxError("Expected types: $types, got ${token.type}")
        }
    }

    private fun parseSimpleCommand(): AST.SimpleCommand {
        val command = when (val token = eat(TokenType.Word, TokenType.Path)) {
            is Token.Word -> token.value
            is Token.Path -> token.value
            else -> throw SyntaxError("Expected command name or path")
        }
        val arguements = mutableListOf<Token>()
        while (lookahead is Token.Word) {
            arguements.add(eat(TokenType.Word))
        }
        return AST.SimpleCommand(
            name = command,
            arguments = arguements.map {
                when (it) {
                    is Token.Word -> AST.WordArgument(it.value)
                    else -> throw SyntaxError("Unexpected argument type: $it")
                }
            })
    }
}

/* AI generated parser.
class Parser(private val tokens: List<Token>) {
    private var position = 0

    fun parse(): Statement {
        return parseSequence()
    }

    private fun parseSequence(): Statement {
        var left = parseExpression()

        while (match(TokenType.Semicolon, TokenType.And, TokenType.Or)) {
            val operator = previous()
            val right = parseExpression()
            left = when (operator.type) {
                TokenType.Semicolon -> Sequence(left, right)
                TokenType.And -> And(left, right)
                TokenType.Or -> Or(left, right)
                else -> throw SyntaxError("Unknown operator ${operator.type}")
            }
        }

        return left
    }

    private fun parseExpression(): Statement {
        return parsePipeline()
    }

    private fun parsePipeline(): Statement {
        val commands = mutableListOf<Command>()
        commands.add(parseCommand())

        while (match(TokenType.Pipe)) {
            commands.add(parseCommand())
        }

        return if (commands.size == 1) commands[0] else Pipeline(commands)
    }

    private fun parseCommand(): Command {
        val wordToken = consume(TokenType.Word, "Expected command name")
        val name = (wordToken as Token.Word).value
        val arguments = mutableListOf<Argument>()

        while (check(TokenType.Word, TokenType.Dollar, TokenType.LeftParen)) {
            when (peek().type) {
                TokenType.Word -> {
                    val wordArg = (advance() as Token.Word).value
                    arguments.add(WordArgument(wordArg))
                }

                TokenType.Dollar -> {
                    arguments.add(parseVariableSubstitution())
                }

                TokenType.LeftParen -> {
                    arguments.add(parseCommandSubstitution())
                }

                else -> throw SyntaxError("Unexpected token in arguments: ${peek()}")
            }
        }

        return SimpleCommand(name, arguments)
    }

    private fun parseVariableSubstitution(): Argument {
        consume(TokenType.Dollar, "Expected '$' for variable substitution")

        val varNameToken = consume(TokenType.Word, "Expected variable name after '$'")
        return VariableSubstitution((varNameToken as Token.Word).value)
    }

    private fun parseCommandSubstitution(): Argument {
        return when (peek().type) {
            TokenType.LeftParen -> {
                val commandStatement = parseCommandSubstitutionContent(TokenType.LeftParen)
                consume(TokenType.RightParen, "Expected closing ')' for command substitution")
                CommandSubstitution(commandStatement)
            }

            else -> throw SyntaxError("Expected '(' for command substitution at position $position")
        }
    }

    private fun parseCommandSubstitutionContent(startToken: TokenType): Statement {
        val tokensForSub = mutableListOf<Token>()
        var nested = 0

        while (!isAtEnd()) {
            when (peek().type) {
                TokenType.LeftParen -> {
                    nested++
                    tokensForSub.add(advance())
                }

                TokenType.RightParen -> {
                    if (nested == 0) break
                    nested--
                    tokensForSub.add(advance())
                }

                else -> {
                    tokensForSub.add(advance())
                }
            }
        }

        if (tokensForSub.isEmpty()) {
            throw SyntaxError("Empty command substitution at position $position")
        }

        // Parse the tokens for the substitution
        val subParser = Parser(tokensForSub + Token.EOF)
        return subParser.parse()
    }

    // Utility Parsing Methods

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    // fixme match can not utilize the vararg of check
    private fun check(vararg types: TokenType): Boolean {
        if (isAtEnd()) return false
        return types.any { it == peek().type }
    }

    private fun consume(type: TokenType, errorMessage: String): Token {
        if (check(type)) return advance()
        throw SyntaxError("$errorMessage at position $position")
    }

    private fun advance(): Token {
        if (!isAtEnd()) position++
        return previous()
    }

    private fun peek(): Token {
        return tokens[position]
    }

    private fun previous(): Token {
        return tokens[position - 1]
    }

    private fun isAtEnd(): Boolean {
        return peek() is Token.EOF
    }
}
*/
// Custom exception for syntax errors
class SyntaxError(message: String) : Exception(message)
