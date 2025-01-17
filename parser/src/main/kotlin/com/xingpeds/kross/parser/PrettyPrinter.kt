package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST
import com.xingpeds.kross.entities.ASTVisitor

class PrettyPrinter : ASTVisitor<String> {
    override fun visitProgram(program: AST.Program): String {
        return program.commands.joinToString("\n") { it.accept(this) }
    }

    override fun visitPipeline(pipeline: AST.Command.Pipeline): String {
        return pipeline.commands.joinToString(" | ") { it.accept(this) }
    }

    override fun visitAnd(and: AST.Command.And): String {
        return "(${and.left.accept(this)} && ${and.right.accept(this)})"
    }

    override fun visitOr(or: AST.Command.Or): String {
        return "(${or.left.accept(this)} || ${or.right.accept(this)})"
    }

    override fun visitSimpleCommand(simple: AST.SimpleCommand): String {
        val command: String = simple.name.accept(this)
        val args = simple.arguments.joinToString(" ") { it.accept(this) }
        return "$command $args".trim()
    }

    override fun visitWordArgument(arg: AST.Argument.WordArgument): String {
        return arg.value
    }

    override fun visitVariableSubstitution(arg: AST.Argument.VariableSubstitution): String {
        return "\$${arg.variableName}"
    }

    override fun visitCommandSubstitution(arg: AST.Argument.CommandSubstitution): String {
        return "(${arg.commandLine.accept(this)})"
    }

    override fun visitDoubleQuoteWithVar(doubleQuoteWithVar: AST.Argument.DoubleQuoteWithVar): String {
        return "\"${doubleQuoteWithVar.text}\""
    }

    override fun visitGlob(glob: AST.Argument.Glob): String {
        return "${glob.text}"
    }

    override fun visitRecursiveGlob(recursiveGlob: AST.Argument.RecursiveGlob): String {
        return "${recursiveGlob.text}"
    }

    override fun visitCommandIdentifier(commandIdentifier: AST.CommandIdentifier): String {
        return commandIdentifier.identifier
    }

    // Implement other argument visit methods as needed.
}