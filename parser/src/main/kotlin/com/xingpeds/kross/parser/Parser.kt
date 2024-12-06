package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

/**
input          ::= sequence

sequence       ::= pipeline { ; pipeline }

operator       ::= '&&' | '||'

pipeline       ::= binaryCommand { '|' binaryCommand }

binaryCommand ::= command { operator binaryCommand }

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
        return AST.Program(sequence())
    }

    private fun sequence(): AST.Sequence {
        val list = mutableListOf<AST.Statement>()
        list.add(pipeline())
        while (lookahead is Token.Semicolon) {
            eat(TokenType.Semicolon)
            list.add(pipeline())
        }
        return AST.Sequence(list)
    }

    private fun pipeline(): AST.Statement {
        val list = mutableListOf<AST.Command>()
        list.add(command())
        while (lookahead is Token.Pipe) {
            eat(TokenType.Pipe)
            list.add(command())
        }
        if (list.size == 1) {
            return list.first()
        }
        return AST.Pipeline(list)
    }

    private fun command(): AST.Command {
        val left: AST.SimpleCommand = simpleCommand()
        return when (lookahead) {
            is Token.And -> {
                and(left)
            }

            is Token.Or -> {
                or(left)
            }

            else -> left
        }
    }

    private fun simpleCommand(): AST.SimpleCommand {
        val commandNameToken = eat(TokenType.Word, TokenType.Path)
        val commandName: AST.CommandName = when (commandNameToken) {
            is Token.Word -> AST.CommandName.Word(commandNameToken.value)
            is Token.Path -> AST.CommandName.Path(commandNameToken.value)
            else -> throw SyntaxError("expected a command name or path and got $commandNameToken")
        }
        val args: List<AST.Argument> = arguments()
        return AST.SimpleCommand(commandName, args)
    }

    private val argTokens =
        listOf(
            TokenType.Word,
            TokenType.Dollar,
            TokenType.LeftParen,
            TokenType.SingleQuotedString,
            TokenType.DoubleQuotedString
        )

    private fun arguments(): List<AST.Argument> {
        val args = mutableListOf<AST.Argument>()
        while (argTokens.contains(lookahead.type)) {
            when (lookahead) {
                Token.Dollar -> args.add(variableSubstitution())
                Token.LeftParen -> args.add(commandSubstitution())
                is Token.DoubleQuote -> args.add(quoteArgument())
                is Token.SingleQuote -> args.add(quoteArgument())
                is Token.Word -> args.add(wordArgument())
                else -> throw Exception("unreachable code, guarded by while loop")
            }
        }
        return args
    }

    private fun variableSubstitution(): AST.VariableSubstitution {
        eat(TokenType.Dollar)
        val varNameToken = eat(TokenType.Word) as Token.Word
        return AST.VariableSubstitution(varNameToken.value)
    }

    private fun wordArgument(): AST.WordArgument {
        val word = eat(TokenType.Word) as Token.Word
        return AST.WordArgument(word.value)
    }

    private fun or(left: AST.SimpleCommand): AST.Or {
        eat(TokenType.Or)
        return AST.Or(left, command())
    }

    private fun and(left: AST.SimpleCommand): AST.And {
        eat(TokenType.And)
        return AST.And(left, command())
    }


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


    private fun advance(): Token = runBlocking {
        println("Starting advance method")
        val token = lookahead
        lookahead = tokens.first()
        return@runBlocking token
    }

    private fun commandSubstitution(): AST.CommandSubstitution {
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


    private fun quoteArgument(): AST.WordArgument {
        val token = eat(TokenType.DoubleQuotedString, TokenType.SingleQuotedString)
        if (token is Token.Literal) {
            return AST.WordArgument(token.value)
        } else {
            throw SyntaxError("Unexpected token: $token")
        }
    }

}

// Custom exception for syntax errors
class SyntaxError(message: String) : Exception(message)
