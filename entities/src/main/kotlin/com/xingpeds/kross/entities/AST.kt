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
    abstract fun <R> accept(visitor: ASTVisitor<R>): R

    @Serializable
    data class Program(val commands: Sequence) : AST() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitProgram(this)
    }


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
        data class Pipeline(val commands: List<SimpleCommand>) : Command() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPipeline(this)
        }

        @Serializable
        data class And(val left: Command, val right: Command) : Command() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitAnd(this)
        }

        @Serializable
        data class Or(val left: Command, val right: Command) : Command() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitOr(this)
        }
    }


    /**
     * A simple command is a single executable plus arguments.
     * e.g. `echo hello world` or `grep foo`
     */
    @Serializable
    data class SimpleCommand(
        val name: CommandName,
        val arguments: List<Argument> = emptyList()
    ) : AST() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitSimpleCommand(this)
    }

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
        data class WordArgument(val value: String) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitWordArgument(this)
        }

        @Serializable
        data class VariableSubstitution(val variableName: String) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitVariableSubstitution(this)
        }

        @Serializable
        data class CommandSubstitution(val commandLine: Program) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitCommandSubstitution(this)
        }

        @Serializable
        data class DoubleQuoteWithVar(val text: String) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitDoubleQuoteWithVar(this)
        }

        @Serializable
        data class Glob(val text: String) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitGlob(this)
        }

        @Serializable
        data class RecursiveGlob(val text: String) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitRecursiveGlob(this)
        }
    }
}