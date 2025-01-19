package com.xingpeds.kross.entities

interface ASTVisitor<T> {
    fun visitProgram(program: AST.Program): T
    fun visitPipeline(pipeline: AST.Command.Pipeline): T
    fun visitAnd(and: AST.Command.And): T
    fun visitOr(or: AST.Command.Or): T
    fun visitSimpleCommand(simpleCommand: AST.SimpleCommand): T
    fun visitWordArgument(wordArgument: AST.Argument.WordArgument): T
    fun visitVariableSubstitution(variableSubstitution: AST.Argument.VariableSubstitution): T
    fun visitCommandSubstitution(commandSubstitution: AST.Argument.CommandSubstitution): T
    fun visitDoubleQuoteWithVar(doubleQuoteWithVar: AST.Argument.DoubleQuoteWithVar): T
    fun visitGlob(glob: AST.Argument.Glob): T
    fun visitRecursiveGlob(recursiveGlob: AST.Argument.RecursiveGlob): T
    fun visitCommandIdentifier(commandIdentifier: AST.CommandIdentifier): T

}
