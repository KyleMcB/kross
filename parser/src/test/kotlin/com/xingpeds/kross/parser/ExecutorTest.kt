//package com.xingpeds.kross.parser
//
//import java.io.BufferedReader
//import kotlin.test.Test
//
//class ExecutorTest {
//    @Test
//    fun simpleEcho() {
//        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.WordArgument("hello world")))))
//        val executor = Executor(ast)
//        executor.execute()
//    }
//
//    @Test
//    fun commandsub() {
//        val subcommand = AST.Program(listOf(AST.SimpleCommand(name = "which", listOf(AST.WordArgument("fish")))))
//        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.CommandSubstitution(subcommand)))))
//        val executor = Executor(ast)
//        executor.execute()
//    }
//
//    @Test
//    fun variableSubstitution() {
//        val env = mapOf("hello" to "world")
//        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.VariableSubstitution("hello")))))
//        val executor = Executor(ast, env = env)
//        executor.execute()
//    }
//
//    @Test
//    fun processbuildertest() {
//        val process = ProcessBuilder("echo", "hello world")
//            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Ensure output is accessible
//            .start() // Start the process
//
//        // Read the process's output
//        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
//
//        process.waitFor() // Wait for the process to complete
//        println(output.trim()) // Trim to remove any trailing newline
//    }
//
//    @Test
//    fun stdin() {
//        val process = ProcessBuilder("cat")
//            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
//            .redirectInput(ProcessBuilder.Redirect.PIPE)  // Redirect stdin to a pipe
//            .start() // Start the process
//
//        // Write to the process's standard input
//        process.outputStream.bufferedWriter().use { writer ->
//            writer.write("hello there\n") // Send the input
//        }
//
//        // Read the process's standard output
//        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
//
//        process.waitFor() // Wait for the process to complete
//        println(output.trim()) // Trim to remove any trailing newline
//    }
//
//    @Test
//    fun overAPipe() {
//        val echoProcess = ProcessBuilder("echo", "over a pipe")
//            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
//            .start()
//
//        // Second process: cat
//        val catProcess = ProcessBuilder("cat")
//            .redirectInput(ProcessBuilder.Redirect.PIPE)  // Redirect stdin to a pipe
//            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
//            .start()
//
//        // Connect the output of echoProcess to the input of catProcess
//        echoProcess.inputStream.copyTo(catProcess.outputStream)
//        catProcess.outputStream.close() // Close catProcess's input stream after piping
//        // Capture the output of catProcess
//        val result = catProcess.inputStream.bufferedReader().use { it.readText() }
//
//        // Wait for both processes to complete
//        echoProcess.waitFor()
//        catProcess.waitFor()
//
//        println(result.trim())
//
//    }
//
//    @Test
//    fun manuel() {
//        val executor = Executor(AST.Program(emptyList()))
//        val command = AST.SimpleCommand("echo", listOf(AST.WordArgument("hello world")))
//        val output = StringBuilder()
//        executor.simpleCommand(command = command, output = output.asOutputStream())
//        println("Captured Output: ${output.toString().trim()}")
//
//
//    }
//
//    @Test
//    fun stdinManuel() {
//        val executor = Executor(AST.Program(emptyList()))
//        val command = AST.SimpleCommand("cat", emptyList())
//        val input = "hello there".byteInputStream()
//        val output = StringBuilder()
//        executor.simpleCommand(command, input = input, output = output.asOutputStream())
//        println("Captured Output: ${output.toString().trim()}")
//    }
//}