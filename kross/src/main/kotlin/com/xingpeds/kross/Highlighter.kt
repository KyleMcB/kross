package com.xingpeds.kross

import com.xingpeds.kross.entities.AST
import com.xingpeds.kross.entities.ASTVisitor

data class Highlight(val range: IntRange, val style: TextStyle)
sealed interface TextStyle {
    companion object {
        val entries = listOf(
            Command.Valid,
            Command.Invalid,
            Command.PotentialCommand,
            Argument,
            Operator,
            Variable,
            ResolvableArgument,
            RecursiveArgument
        )
    }

    sealed interface Command : TextStyle {
        data object Valid : Command
        data object Invalid : Command
        data object PotentialCommand : Command
    }

    data object Argument : TextStyle
    data object Operator : TextStyle
    data object Variable : TextStyle
    data object ResolvableArgument : TextStyle
    data object RecursiveArgument : TextStyle
}


class Highlighter(private val isCommandValid: (String) -> TextStyle.Command) : ASTVisitor<List<Highlight>> {
    override fun visitProgram(program: AST.Program): List<Highlight> {
        return program.commands.foldIndexed(emptyList()) { index, acc, command ->
            acc + if (index == 0) {
                command.accept(this)
            } else {
                val commandHighlights = command.accept(this)
                val previous = program.commands[index - 1]
                val operatorRange = previous.sourceLocation.last..command.sourceLocation.first
                val operatorHighlight = Highlight(operatorRange, TextStyle.Operator)
                commandHighlights + operatorHighlight
            }
        }
    }

    override fun visitPipeline(pipeline: AST.Command.Pipeline): List<Highlight> {
        return pipeline.commands.foldIndexed(emptyList()) { index, acc, simpleCommand ->
            acc + if (index == 0) {
                simpleCommand.accept(this)
            } else {
                val previous = pipeline.commands[index - 1]
                val operatorRange = previous.sourceLocation.last..simpleCommand.sourceLocation.first
                simpleCommand.accept(this) + Highlight(operatorRange, TextStyle.Operator)
            }
        }
    }

    override fun visitAnd(and: AST.Command.And): List<Highlight> = and.left.accept(this) + Highlight(
        and.left.sourceLocation.last..and.right.sourceLocation.first,
        TextStyle.Operator
    ) + and.right.accept(this)

    override fun visitOr(or: AST.Command.Or): List<Highlight> {
        val start = or.left.sourceLocation.last
        val end = or.right.sourceLocation.first
        val range = start..end
        val op = Highlight(range, TextStyle.Operator)
        return or.left.accept(this) + op + or.right.accept(this)
    }

    override fun visitSimpleCommand(simpleCommand: AST.SimpleCommand): List<Highlight> =
        simpleCommand.name.accept(this) + simpleCommand.arguments.flatMap { it.accept(this) }

    override fun visitWordArgument(wordArgument: AST.Argument.WordArgument): List<Highlight> =
        listOf(Highlight(wordArgument.sourceLocation, TextStyle.Argument))

    override fun visitVariableSubstitution(variableSubstitution: AST.Argument.VariableSubstitution): List<Highlight> =
        Highlight(variableSubstitution.sourceLocation, TextStyle.Variable).toList()

    override fun visitCommandSubstitution(commandSubstitution: AST.Argument.CommandSubstitution): List<Highlight> {
        return visitProgram(commandSubstitution.commandLine)
    }

    override fun visitDoubleQuoteWithVar(doubleQuoteWithVar: AST.Argument.DoubleQuoteWithVar): List<Highlight> {
        return Highlight(doubleQuoteWithVar.sourceLocation, TextStyle.ResolvableArgument).toList()
    }

    override fun visitGlob(glob: AST.Argument.Glob): List<Highlight> {
        return Highlight(glob.sourceLocation, TextStyle.ResolvableArgument).toList()
    }

    override fun visitRecursiveGlob(recursiveGlob: AST.Argument.RecursiveGlob): List<Highlight> {
        return Highlight(recursiveGlob.sourceLocation, TextStyle.RecursiveArgument).toList()
    }

    override fun visitCommandIdentifier(commandIdentifier: AST.CommandIdentifier): List<Highlight> {
        return Highlight(commandIdentifier.sourceLocation, isCommandValid(commandIdentifier.identifier)).toList()
    }
}

private fun <R> R.toList(): List<R> = listOf(this)