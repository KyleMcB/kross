package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

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
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val lexer: Lexer = Lexer(input, coroutineScope),
    private val tokens: Flow<Token> = lexer.tokens()
) : CoroutineScope by coroutineScope {
    private var lookahead: Token

    init {
        runBlocking {
            lookahead = tokens.first()
        }
    }

    fun parse(): AST.Program {
        println("Starting parse method")
        return program()
    }

    private fun program(): AST.Program {
        println("Starting program method")
        return AST.Program(parseCommand())
    }

    private fun parseCommand(): List<AST.Command> {
        println("Starting parseCommand method")
//        val simpleCommand = parseSimpleCommand()
//        val commandList = mutableListOf<AST.Command>()
//        commandList.add(simpleCommand)
//        while (tokenIsOperator()) {
//            eat(TokenType.Semicolon)
//            commandList.add(parseSimpleCommand())
//        }
//        return commandList
        val commandList = mutableListOf<AST.Command>()
        var command: AST.Command = parseSimpleCommand()
        while (tokenIsOperator()) {
            when (lookahead) {
                Token.And -> command = parseAndCommand(command)
                Token.Or -> command = parseOrCommand(command)
                Token.Pipe -> {
                    command = parsePipeline(command)
                }

                Token.Semicolon -> {
                    eat(TokenType.Semicolon)
                    commandList.add(command)
                    command = parseSimpleCommand()
                }

                else -> throw SyntaxError("Unexpected token: $lookahead")
            }
        }
        commandList.add(command)
        return commandList
    }

    private fun parseOrCommand(left: AST.Command): AST.Or {
        println("Starting parseOrCommand method")
        eat(TokenType.Or)
        return AST.Or(left, parseSimpleCommand())
    }

    private fun parseAndCommand(left: AST.Command): AST.And {
        println("Starting parseAndCommand method")
        eat(TokenType.And)
        return AST.And(left, parseSimpleCommand())
    }

    private fun parsePipeline(command: AST.Command): AST.Pipeline {
        println("Starting parsePipeline method")
        eat(TokenType.Pipe)
        return if (command is AST.Pipeline) {
            AST.Pipeline(command.commands + parseSimpleCommand())
        } else {
            AST.Pipeline(listOf(command, parseSimpleCommand()))
        }
    }

    private fun tokenIsOperator(): Boolean = operators.contains(lookahead.type)
    private val operators = listOf(TokenType.And, TokenType.Or, TokenType.Semicolon, TokenType.Pipe)
    private fun eat(vararg types: TokenType): Token = eat(types.toList())
    private fun eat(expectedTypes: List<TokenType>): Token {
        println("Starting eat method")
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

    private val argTokens =
        listOf(
            TokenType.Word,
            TokenType.Dollar,
            TokenType.LeftParen,
            TokenType.SingleQuotedString,
            TokenType.DoubleQuotedString
        )

    private fun parseSimpleCommand(): AST.SimpleCommand {
        println("Starting parseSimpleCommand method")
        val command = when (val token = eat(TokenType.Word, TokenType.Path)) {
            is Token.Word -> token.value
            is Token.Path -> token.value
            else -> throw SyntaxError("Expected command name or path")
        }
        val arguments = mutableListOf<AST.Argument>()
        while (argTokens.contains(lookahead.type)) {
            val arg: AST.Argument = when (lookahead) {
                is Token.Dollar -> parseVariableSubstitution()
                is Token.LeftParen -> parseCommandSubstitution()
                else -> parseWordArgument()
            }
            arguments.add(arg)
        }
        return AST.SimpleCommand(
            name = command,
            arguments = arguments
        )
    }

    private fun advance(): Token = runBlocking {
        println("Starting advance method")
        val token = lookahead
        lookahead = tokens.first()
        return@runBlocking token
    }

    private fun parseCommandSubstitution(): AST.CommandSubstitution {
        println("Starting parseCommandSubstitution method")
        eat(TokenType.LeftParen)
        val tokensForSub = mutableListOf<Token>()
        var nested = 0

        while (lookahead !is Token.EOF) {
            when (lookahead) {
                Token.LeftParen -> {
                    nested++
                    tokensForSub.add(eat(TokenType.LeftParen))
                }

                Token.RightParen -> {
                    if (nested == 0) {
                        eat(TokenType.RightParen)
                        break
                    }
                    nested--
                    tokensForSub.add(advance())
                }

                else -> {
                    tokensForSub.add(advance())
                }
            }
        }

        if (tokensForSub.isEmpty()) {
            throw SyntaxError("Empty command substitution at position $lookahead")
        }

        var subCursor = 0
        val subFlow = flow<Token> {
            while (subCursor < tokensForSub.size) {
                val token = tokensForSub[subCursor]
                subCursor += 1
                emit(token)
            }
            emit(Token.EOF)
        }
        // Parse the tokens for the substitution
        val subParser = Parser(input = "", tokens = subFlow)
        return AST.CommandSubstitution(subParser.parse())
    }

    private fun parseWordArgument(): AST.Argument {
        return when (lookahead) {
            is Token.Word -> return parseSimpleWordArgument()
            is Token.SingleQuote, is Token.DoubleQuote -> parseQuoteArgument()
            else -> throw SyntaxError("Unexpected token: $lookahead")
        }
    }

    private fun parseSimpleWordArgument(): AST.WordArgument {
        val token = eat(TokenType.Word) as Token.Word
        return AST.WordArgument(token.value)
    }

    //
    private val quotes = listOf(TokenType.DoubleQuotedString, TokenType.SingleQuotedString)
    private fun isQuote(): Boolean = quotes.contains(lookahead.type)
    private fun parseQuoteArgument(): AST.WordArgument {
        val token = eat(TokenType.DoubleQuotedString, TokenType.SingleQuotedString)
        if (token is Token.Literal) {
            return AST.WordArgument(token.value)
        } else {
            throw SyntaxError("Unexpected token: $token")
        }
    }

    private fun parseVariableSubstitution(): AST.VariableSubstitution {
        println("Starting parseVariableSubstitution method")
        eat(TokenType.Dollar)
        val varToken = eat(TokenType.Word) as Token.Word
        return AST.VariableSubstitution(varToken.value)
    }

}

// Custom exception for syntax errors
class SyntaxError(message: String) : Exception(message)
