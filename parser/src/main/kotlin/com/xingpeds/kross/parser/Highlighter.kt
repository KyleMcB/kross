package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.AST

typealias Processor = suspend (AST) -> Unit

class Highlighter(private val processor: Processor) {
    suspend fun process(ast: AST.Program) {

    }
}