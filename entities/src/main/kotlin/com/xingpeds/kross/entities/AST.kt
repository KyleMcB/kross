package com.xingpeds.kross.entities


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


sealed class AST {
    abstract val sourceLocation: IntRange
    abstract fun <R> accept(visitor: ASTVisitor<R>): R


    data class Program(val commands: Sequence) : AST() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitProgram(this)
        override val sourceLocation: IntRange
            get() {
                val first = commands.firstOrNull()?.sourceLocation?.first ?: 0
                val last = commands.lastOrNull()?.sourceLocation?.last ?: 0
                return first..last
            }
    }


    /**
     * A command is one or more pipelines connected by logical operators (&& or ||).
     * We'll model this as a tree: a command can be just one pipeline, or a logical node (And/Or)
     * connecting two Commands.
     */

    sealed class Command : AST() {
        /**
         * A pipeline is a sequence of simple commands connected by `|`.
         * e.g. cmd1 | cmd2 | cmd3
         */

        data class Pipeline(val commands: List<SimpleCommand>) : Command() {
            override val sourceLocation: IntRange
                get() = commands.first().sourceLocation.first..commands.last().sourceLocation.last

            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitPipeline(this)
        }


        data class And(val left: Command, val right: Command) : Command() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitAnd(this)
            override val sourceLocation: IntRange
                get() = left.sourceLocation.first..right.sourceLocation.last
        }


        data class Or(val left: Command, val right: Command) : Command() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitOr(this)
            override val sourceLocation: IntRange
                get() = left.sourceLocation.first..right.sourceLocation.last
        }
    }


    /**
     * A simple command is a single executable plus arguments.
     * e.g. `echo hello world` or `grep foo`
     */

    data class SimpleCommand(
        val name: CommandIdentifier,
        val arguments: List<Argument> = emptyList()
    ) : AST() {
        override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitSimpleCommand(this)
        override val sourceLocation: IntRange
            get() = name.sourceLocation.first..(arguments.lastOrNull()?.sourceLocation?.last
                ?: name.sourceLocation.last)
    }

    data class CommandIdentifier(val identifier: String, override val sourceLocation: IntRange) : AST() {
        override fun <R> accept(visitor: ASTVisitor<R>): R {
            return visitor.visitCommandIdentifier(this)
        }
    }

    /**
     * Arguments are either a plain word or some form of substitution.
     */

    sealed class Argument : AST() {

        data class WordArgument(val value: String, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitWordArgument(this)
        }


        data class VariableSubstitution(val variableName: String, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitVariableSubstitution(this)
        }


        data class CommandSubstitution(val commandLine: Program, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitCommandSubstitution(this)
        }


        data class DoubleQuoteWithVar(val text: String, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitDoubleQuoteWithVar(this)
        }


        data class Glob(val text: String, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitGlob(this)
        }


        data class RecursiveGlob(val text: String, override val sourceLocation: IntRange) : Argument() {
            override fun <R> accept(visitor: ASTVisitor<R>): R = visitor.visitRecursiveGlob(this)
        }
    }
}