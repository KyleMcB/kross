package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

    fun parse(): AST {
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

    private val argTokens = listOf(TokenType.Word, TokenType.Dollar, TokenType.LeftParen)
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
                is Token.Word -> parseWordArgument()
                else -> throw SyntaxError("Unexpected token: $lookahead")
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
        println("Starting parseWordArgument method")
        val wordToken = eat(TokenType.Word) as Token.Word
        return AST.WordArgument(wordToken.value)
    }

    private fun parseVariableSubstitution(): AST.VariableSubstitution {
        println("Starting parseVariableSubstitution method")
        eat(TokenType.Dollar)
        val varToken = eat(TokenType.Word) as Token.Word
        return AST.VariableSubstitution(varToken.value)
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
