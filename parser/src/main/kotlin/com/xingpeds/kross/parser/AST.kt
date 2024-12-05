package com.xingpeds.kross.parser

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
sealed class AST {

    data class Program(val statements: List<AST.Command>) : AST()

    // Represents a single command with arguments and possible substitutions
    sealed class Command : AST()

    // Represents logical AND (&&) between statements
    data class And(
        val left: AST,
        val right: AST
    ) : Command()

    // Represents logical OR (||) between statements
    data class Or(
        val left: AST,
        val right: AST
    ) : Command()

    // Represents a pipeline of commands connected by |
    data class Pipeline(
        val commands: List<Command>
    ) : Command()

    data class SimpleCommand(
        val name: String,
        val arguments: List<Argument> = emptyList()
    ) : Command()

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