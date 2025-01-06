package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

/*
input          ::= sequence
sequence       ::= command { ';' command }
command        ::= pipeline { operator pipeline }
operator       ::= '&&' | '||'
pipeline       ::= simpleCommand { '|' simpleCommand }
simpleCommand  ::= WORD { argument }
argument       ::= WORD | substitution
substitution   ::= variable_substitution | command_substitution
variable_substitution ::= '$' '{'? WORD '}'?
command_substitution ::= '(' command_line ')'
command_line   ::= input
*/
class Parser {
    var iterator: Iterator<Token> = emptyList<Token>().iterator()
    var lookahead: Token = Token.EOF(0..0)
    private fun peek() = lookahead
    private fun advance(): Token {
        val token = lookahead
        lookahead = iterator.next()
        return token
    }

    private fun eat(vararg tokens: TokenType): Token {
        val token = peek()
        if (token is Token.EOF) {
            return token
        }
        if (tokens.contains(token.type)) {
            try {
                return advance()
            } catch (e: NoSuchElementException) {
                throw SyntaxError("Unexpected end of input, expected ${tokens.joinToString(", ")}")
            }
        } else {
            throw Exception("Unexpected token $token\nExpected one of ${tokens.joinToString(", ")}")
        }
    }

    suspend fun parse(input: Flow<Token>): AST.Program {
        val tokens = input.toList()
        iterator = tokens.iterator()
        lookahead = iterator.next()
        val program = AST.Program(parseSequence())
        eat(TokenType.EOF)
        return program
    }

    private suspend fun parseSequence(): List<AST.Command> {
        val commands = mutableListOf<AST.Command>()
        commands.add(parseCommand())
        while (peek() is Token.Semicolon) {
            eat(TokenType.Semicolon)
            commands.add(parseCommand())
        }
        return commands
    }

    private suspend fun parseCommand(): AST.Command {
        var command: AST.Command = parsePipeline()
        while (peek() is Token.Operator) {
            val token = peek() as Token.Operator
            command = when (token) {
                is Token.And -> {
                    eat(TokenType.And)
                    AST.Command.And(left = command, right = parseCommand())
                }

                is Token.Or -> {
                    eat(TokenType.Or)
                    AST.Command.Or(left = command, right = parseCommand())
                }
            }
        }
        return command
    }

    private suspend fun parsePipeline(): AST.Command.Pipeline {
        val commands = mutableListOf<AST.SimpleCommand>()
        commands.add(parseSimpleCommand())
        while (peek() is Token.Pipe) {
            eat(TokenType.Pipe)
            commands.add(parseSimpleCommand())
        }
        return AST.Command.Pipeline(commands)
    }

    private suspend fun parseSimpleCommand(): AST.SimpleCommand {
        val name: AST.CommandName = parseCommandName()
        val arguments = parseArgumentList()

        return AST.SimpleCommand(name = name, arguments = arguments)
    }

    private suspend fun parseArgumentList(): List<AST.Argument> {
        val args = mutableListOf<AST.Argument>()
        while (peek() is Token.Dollar || peek() is Token.LeftParen || peek() is Token.Word || peek() is Token.SingleQuote || peek() is Token.DoubleQuote) {
            when (peek()) {
                is Token.Dollar -> args.add(parseVariable())
                is Token.Word -> args.add(parseWordArgument())
                is Token.LeftParen -> args.add(parseCommandSubstitution())
                is Token.SingleQuote -> args.add(parseSingleQuote())
                is Token.DoubleQuote -> args.add(parseDoubleQuote())
                else -> throw Exception("") // this line is unreachable because of the while loop condition
            }
        }
        return args
    }

    private fun parseDoubleQuote(): AST.Argument.WordArgument {
        val token = eat(TokenType.DoubleQuotedString) as Token.DoubleQuote
        return AST.Argument.WordArgument(token.value)
    }

    private suspend fun parseSingleQuote(): AST.Argument.WordArgument {
        val token = eat(TokenType.SingleQuotedString) as Token.SingleQuote
        return AST.Argument.WordArgument(token.value)
    }

    private suspend fun parseVariable(): AST.Argument.VariableSubstitution {
        eat(TokenType.Dollar)
        var cleanUp: () -> Unit = {}
        if (peek() is Token.LeftBracket) {
            eat(TokenType.LeftBracket)
            cleanUp = { val nothing = eat(TokenType.RightBracket) }
        }
        // parse the variable
        val varNameToken = eat(TokenType.Word) as Token.Word
        cleanUp()
        return AST.Argument.VariableSubstitution(varNameToken.value)
    }

    private suspend fun parseCommandSubstitution(): AST.Argument.CommandSubstitution {
        eat(TokenType.LeftParen)
        val tokensForSub = mutableListOf<Token>()
        var nested = 0
        while (peek() !is Token.EOF || nested > 0) {
            when (peek()) {
                is Token.LeftParen -> {
                    nested++
                    tokensForSub.add(eat(TokenType.LeftParen))
                }

                is Token.RightParen -> {
                    if (nested == 0) {
                        eat(TokenType.RightParen)
                        break
                    } else {
                        nested--
                        tokensForSub.add(advance())
                    }
                }

                is Token.EOF -> throw SyntaxError("expected ) but got EOF")
                else -> tokensForSub.add(advance())
            }
        }
        tokensForSub.add(Token.EOF(1..1)) //fix me not sure if this will be an issue later
        val subFlow = tokensForSub.asFlow()
        val subParser = Parser()
        val subProgram = subParser.parse(subFlow)
        return AST.Argument.CommandSubstitution(subProgram)
    }

    private fun parseWordArgument(): AST.Argument.WordArgument {
        val token = eat(TokenType.Word) as Token.Word
        return AST.Argument.WordArgument(token.value)
    }

    private fun parseCommandName(): AST.CommandName {
        val token = eat(TokenType.Word)
        return if (token is Token.Word) {
            AST.CommandName.Word(token.value)
        } else throw SyntaxError("expected a command name or path and got $token")
    }
}

class SyntaxError(message: String) : Exception(message)
