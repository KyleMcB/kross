//package com.xingpeds.kross.parser
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.ByteArrayOutputStream
//import java.io.InputStream
//import java.io.OutputStream
//
//data class Streams(
//    val input: InputStream = System.`in`,
//    val output: OutputStream = System.out,
//    val error: OutputStream = System.err
//) {}
//
//class Executor(
//    val program: AST.Program,
//    val localCommands: Set<String> = emptySet(),
//    val environment: Map<String, String> = emptyMap(),
//    val streams: Streams = Streams(),
//) {
//    private val returnValues = mutableListOf<Int>()
//    suspend fun execute(): List<Int> {
//        exeProgram(program)
//        return returnValues
//    }
//
//    private suspend fun exeProgram(program: AST.Program) {
//        exeSequence(program.statements)
//    }
//
//    private suspend fun exeSequence(sequence: AST.Sequence) {
//        for (statement in sequence.statements) {
//            exeStatement(statement)
//        }
//    }
//
//    private suspend fun exeStatement(statement: AST.Statement) {
//        when (statement) {
//            is AST.And -> exeAnd(statement)
//            is AST.Or -> exeOr(statement)
//            is AST.SimpleCommand -> exeSimpleCommand(statement)
//            is AST.Pipeline -> exePipeline(statement)
//        }
//    }
//
//    private suspend fun exeOr(or: AST.Or): Int {
//        val leftResult = exeSimpleCommand(or.left)
//        if (leftResult != 0) {
//            val right = or.right ?: throw SyntaxError("expected the right side of an OR, found $or")
//            return exeCommand(right, firstStream) * leftResult
//        }
//        return leftResult
//    }
//
//    private suspend fun exeAnd(statement: AST.And): Int {
//        val leftResult = exeSimpleCommand(statement.left)
//        if (leftResult == 0) {
//            val right = statement.right ?: throw SyntaxError("expected a right expression for and got ${statement}")
//            return exeCommand(statement.right, firstStream)
//        }
//        return leftResult
//    }
//
//    private suspend fun exeCommand(command: AST.Command, streams: Streams = this.streams): Int {
//        return when (command) {
//            is AST.And -> exeAnd(command)
//            is AST.Or -> exeOr(command)
//            is AST.SimpleCommand -> exeSimpleCommand(command)
//        }
//    }
//
//    private suspend fun exePipeline(statement: AST.Pipeline) {
//
//        if (statement.commands.size == 2) {
//            exeSmallPipe(statement)
//        }
//    }
//
//    private suspend fun exeSmallPipe(pipe: AST.Pipeline) {
//        val firstCommandOutput = ByteArrayOutputStream()
//        val firstStream = Streams(output = firstCommandOutput)
//
//        // Execute the first command and wait for it to complete
//        exeCommand(pipe.commands[0], streams = firstStream)
//
//        // Use the captured output as input for the next command
//        val secondStream = Streams(
//            input = firstCommandOutput.toByteArray().inputStream(),
//            output = streams.output,
//            error = streams.error
//        )
//
//        // Execute the second command
//        exeCommand(pipe.commands[1], streams = secondStream)
//    }
//
//    private suspend fun exeSimpleCommand(statement: AST.SimpleCommand): Int {
//        return when (statement.name) {
//            is AST.CommandName.Path -> exeExternalCommand(statement.name.value, statement.arguments)
//            is AST.CommandName.Word -> if (localCommands.contains(statement.name.value)) {
//                exeLocalCommand(statement.name.value, statement.arguments)
//            } else exeExternalCommand(statement.name.value, statement.arguments)
//
//        }
//    }
//
//    private suspend fun exeLocalCommand(command: String, args: List<AST.Argument>): Int {
//        TODO()
//    }
//
//    private suspend fun exeExternalCommand(
//        name: String,
//        args: List<AST.Argument>,
//        streams: Streams = this.streams
//    ): Int = coroutineScope {
//        val resolvedArgs: List<String> = resolveArguments(args)
//        val pb = ProcessBuilder(listOf(name) + resolvedArgs)
//        val process = pb.start()
//        try {
//            // Launch coroutines for copying streams
//            val inputJob = launch { streams.input.copyToSuspend(process.outputStream) }
//            val outputJob = launch { process.inputStream.copyToSuspend(streams.output) }
//            val errorJob = launch { process.errorStream.copyToSuspend(streams.error) }
//
//            // Wait for the process to complete
//            val exitCode = process.waitFor()
//
//            // Ensure all jobs finish
//            inputJob.join()
//            outputJob.join()
//            errorJob.join()
//            process.errorStream.close()
//            process.inputStream.close()
//            process.outputStream.close()
//
//            returnValues.add(exitCode)
//            exitCode
//        } finally {
//            process.destroy()
//        }
//    }
//
//    private suspend fun resolveArguments(args: List<AST.Argument>): List<String> {
//        val resolved = mutableListOf<String>()
//        for (arg in args) {
//            when (arg) {
//                is AST.CommandSubstitution -> TODO()
//                is AST.VariableSubstitution -> resolved.add(environment[arg.variableName] ?: "")
//                is AST.WordArgument -> resolved.add(arg.value)
//            }
//        }
//        return resolved
//    }
//}
//
//fun StringBuilder.asOutputStream(): OutputStream = object : OutputStream() {
//    override fun write(b: Int) {
//        append(b.toChar())
//    }
//}
//
//fun StringBuilder.asInputStream(): InputStream = object : InputStream() {
//    private var position = 0 // Tracks the current read position
//
//    override fun read(): Int {
//        // If the position is beyond the StringBuilder length, return -1 (end of stream)
//        if (position >= this@asInputStream.length) {
//            return -1
//        }
//        // Return the character at the current position as an integer and advance the position
//        return this@asInputStream[position++].code
//    }
//
//    override fun read(b: ByteArray, off: Int, len: Int): Int {
//        // If no more data, return -1 (end of stream)
//        if (position >= this@asInputStream.length) {
//            return -1
//        }
//
//        // Calculate how many bytes we can read
//        val bytesToRead = minOf(len, this@asInputStream.length - position)
//        for (i in 0 until bytesToRead) {
//            b[off + i] = this@asInputStream[position++].code.toByte()
//        }
//        return bytesToRead
//    }
//}
//
//suspend fun InputStream.copyToSuspend(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
//    withContext(Dispatchers.IO) {
//        val buffer = ByteArray(bufferSize)
//        var bytesRead: Int
//        while (read(buffer).also { bytesRead = it } >= 0) {
//            out.write(buffer, 0, bytesRead)
//            out.flush()
//        }
//    }
//}