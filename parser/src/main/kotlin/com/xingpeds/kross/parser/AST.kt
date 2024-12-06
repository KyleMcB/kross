package com.xingpeds.kross.parser

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
sealed class AST {

    data class Program(val statements: Sequence) : AST()


    data class Sequence(val statements: List<AST.Statement>) : AST()
    sealed interface Statement

    // Represents a pipeline of commands connected by |
    data class Pipeline(
        val commands: List<Command>
    ) : AST(), Statement

    sealed interface Command : Statement {}
    sealed class BinaryCommand() : AST() {
        abstract val left: SimpleCommand
        abstract val right: Command?
    }

    // Represents logical AND (&&) between statements
    data class And(
        override val left: AST.SimpleCommand,
        override val right: AST.Command?
    ) : BinaryCommand(), Command

    // Represents logical OR (||) between statements
    data class Or(
        override val left: AST.SimpleCommand,
        override val right: AST.Command?
    ) : BinaryCommand(), Command

    data class Single(override val left: SimpleCommand) : BinaryCommand() {
        override val right = null
    }

    sealed class CommandName {
        abstract val value: String

        data class Word(override val value: String) : CommandName()
        data class Path(override val value: String) : CommandName()
    }

    data class SimpleCommand(
        val name: CommandName,
        val arguments: List<Argument> = emptyList()
    ) : AST(), Command

    // Represents an argument which can be a word or a substitution
    sealed class Argument

    data class WordArgument(
        val value: String
    ) : Argument()

    data class VariableSubstitution(
        val variableName: String
    ) : Argument()

    data class CommandSubstitution(
        val commandLine: AST.Program
    ) : Argument()
}