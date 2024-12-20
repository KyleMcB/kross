package com.xingpeds.kross.entities

import kotlinx.serialization.Serializable


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
command_substitution ::= '$(' command_line ')'
command_line   ::= input
*/

typealias Sequence = List<AST.Command>

@Serializable
sealed class AST {
    @Serializable
    data class Program(val commands: Sequence) : AST()


    /**
     * A command is one or more pipelines connected by logical operators (&& or ||).
     * We'll model this as a tree: a command can be just one pipeline, or a logical node (And/Or)
     * connecting two Commands.
     */
    @Serializable
    sealed class Command : AST() {
        /**
         * A pipeline is a sequence of simple commands connected by `|`.
         * e.g. cmd1 | cmd2 | cmd3
         */
        @Serializable
        data class Pipeline(val commands: List<SimpleCommand>) : Command()

        @Serializable
        data class And(val left: Command, val right: Command) : Command()

        @Serializable
        data class Or(val left: Command, val right: Command) : Command()
    }


    /**
     * A simple command is a single executable plus arguments.
     * e.g. `echo hello world` or `grep foo`
     */
    @Serializable
    data class SimpleCommand(
        val name: CommandName,
        val arguments: List<Argument> = emptyList()
    ) : AST()

    @Serializable
    sealed class CommandName {
        abstract val value: String

        @Serializable
        data class Word(override val value: String) : CommandName()
    }

    /**
     * Arguments are either a plain word or some form of substitution.
     */
    @Serializable
    sealed class Argument : AST() {
        @Serializable
        data class WordArgument(val value: String) : Argument()

        @Serializable
        data class VariableSubstitution(val variableName: String) : Argument()

        @Serializable
        data class CommandSubstitution(val commandLine: Program) : Argument()
    }
}