package com.xingpeds.kross.parser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals


class ExecutorTest {
    @Test
    fun simpleEcho() = runTest {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    listOf(
                        AST.SimpleCommand(
                            AST.CommandName.Word("echo"),
                            listOf(AST.Argument.WordArgument("hello world"))
                        )
                    )
                )
            )
        )
        val executor = Executor()
        val output = StringBuilder()
        val streams = Executor.Streams(outputStream = output.asOutputStream())
        executor.execute(ast, streams = streams)
        assertEquals("hello world", output.toString().trim())
    }

    @Test
    fun variableSub() = runTest {
        val env = mapOf("hello" to "world")
        val ast = AST.Program(
            listOf(
                AST.Command.Pipeline(
                    listOf(
                        AST.SimpleCommand(
                            AST.CommandName.Word("echo"),
                            listOf(AST.Argument.VariableSubstitution("hello"))
                        )
                    )
                )
            )
        )
        val executor = Executor()
        executor.execute(ast, env = env)
    }

    @Test
    fun and2() = runTest {
        val ast = AST.Program(
            listOf(

                AST.Command.And(
                    left = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("true")))
                    ),
                    right = AST.Command.Pipeline(
                        listOf(
                            AST.SimpleCommand(
                                AST.CommandName.Word("false"),
                            )
                        )
                    )
                )
            )
        )
        val executor = Executor()
        val returnCodes = executor.execute(ast)
        assertEquals(0, returnCodes[0])
        assertEquals(1, returnCodes[1])
    }

    @Test
    fun and1() = runTest {
        val ast = AST.Program(
            listOf(

                AST.Command.And(
                    left = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("true")))
                    ),
                    right = AST.Command.Pipeline(
                        listOf(
                            AST.SimpleCommand(
                                AST.CommandName.Word("echo"),
                                listOf(AST.Argument.WordArgument("hello"))
                            )
                        )
                    )
                )
            )
        )
        val executor = Executor()
        val returnCodes = executor.execute(ast)
        assertEquals(0, returnCodes[0])
        assertEquals(0, returnCodes[1])
    }

    @Test
    fun and3() = runTest {
        val ast = AST.Program(
            listOf(
                AST.Command.And(
                    left = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("false")))
                    ),
                    right = AST.Command.Pipeline(
                        listOf(
                            AST.SimpleCommand(AST.CommandName.Word("true")),
                        )
                    )
                )
            )
        )

        val executor = Executor()
        val returnCodes = executor.execute(ast)
        assertEquals(1, returnCodes.size)
        assertEquals(1, returnCodes[0])
    }

    @Test
    fun or1() = runTest {

        val ast = AST.Program(
            listOf(
                AST.Command.Or(
                    left = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("true")))
                    ),
                    right = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("false")))
                    )
                )
            )
        )
        val executor = Executor()
        val returnCodes = executor.execute(ast)
        assertEquals(1, returnCodes.size)
        assertEquals(0, returnCodes[0])
    }

    @Test
    fun or2() = runTest {

        val ast = AST.Program(

            listOf(
                AST.Command.Or(
                    left = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("false")))
                    ),
                    right = AST.Command.Pipeline(
                        listOf(AST.SimpleCommand(AST.CommandName.Word("true")))
                    )
                )
            )
        )
        val executor = Executor()
        val returnCodes = executor.execute(ast)
        assertEquals(2, returnCodes.size)
        assertEquals(1, returnCodes[0])
        assertEquals(0, returnCodes[1])
    }

    @Test
    fun seq1() = runTest {
        val ast = AST.Program(
            listOf(
                AST.Command.Pipeline(
                    listOf(AST.SimpleCommand(AST.CommandName.Word("echo"), listOf(AST.Argument.WordArgument("hello")))),
                ),
                AST.Command.Pipeline(
                    listOf(AST.SimpleCommand(AST.CommandName.Word("echo"), listOf(AST.Argument.WordArgument("world")))),
                ),

                )
        )
        val output = StringBuilder()
        val streams = Executor.Streams(
            outputStream = output.asOutputStream(),
        )
        val executor = Executor()
        executor.execute(ast, streams = streams)
        assertEquals("hello\nworld\n", output.toString())
    }

    @Test
    fun pipe1() = runTest {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(AST.Argument.WordArgument("hello there"))
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("cat")
                        )
                    )
                )
            )
        )
        val output = StringBuilder()
        val streams = Executor.Streams(outputStream = output.asOutputStream())
        val executor = Executor()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            executor.execute(ast, streams = streams)
        }.join()
        assertEquals("hello there", output.toString().trim())
    }

////
////    @Test
////    fun commandsub() {
////        val subcommand = AST.Program(listOf(AST.SimpleCommand(name = "which", listOf(AST.WordArgument("fish")))))
////        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.CommandSubstitution(subcommand)))))
////        val executor = Executor(ast)
////        executor.execute()
////    }
////
////    @Test
////    fun variableSubstitution() {
////        val env = mapOf("hello" to "world")
////        val ast = AST.Program(listOf(AST.SimpleCommand(name = "echo", listOf(AST.VariableSubstitution("hello")))))
////        val executor = Executor(ast, env = env)
////        executor.execute()
////    }
////
////    @Test
////    fun processbuildertest() {
////        val process = ProcessBuilder("echo", "hello world")
////            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Ensure output is accessible
////            .start() // Start the process
////
////        // Read the process's output
////        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
////
////        process.waitFor() // Wait for the process to complete
////        println(output.trim()) // Trim to remove any trailing newline
////    }
////
////    @Test
////    fun stdin() {
////        val process = ProcessBuilder("cat")
////            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
////            .redirectInput(ProcessBuilder.Redirect.PIPE)  // Redirect stdin to a pipe
////            .start() // Start the process
////
////        // Write to the process's standard input
////        process.outputStream.bufferedWriter().use { writer ->
////            writer.write("hello there\n") // Send the input
////        }
////
////        // Read the process's standard output
////        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
////
////        process.waitFor() // Wait for the process to complete
////        println(output.trim()) // Trim to remove any trailing newline
////    }
////
////    @Test
////    fun overAPipe() {
////        val echoProcess = ProcessBuilder("echo", "over a pipe")
////            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
////            .start()
////
////        // Second process: cat
////        val catProcess = ProcessBuilder("cat")
////            .redirectInput(ProcessBuilder.Redirect.PIPE)  // Redirect stdin to a pipe
////            .redirectOutput(ProcessBuilder.Redirect.PIPE) // Redirect stdout to a pipe
////            .start()
////
////        // Connect the output of echoProcess to the input of catProcess
////        echoProcess.inputStream.copyTo(catProcess.outputStream)
////        catProcess.outputStream.close() // Close catProcess's input stream after piping
////        // Capture the output of catProcess
////        val result = catProcess.inputStream.bufferedReader().use { it.readText() }
////
////        // Wait for both processes to complete
////        echoProcess.waitFor()
////        catProcess.waitFor()
////
////        println(result.trim())
////
////    }
////
////    @Test
////    fun manuel() {
////        val executor = Executor(AST.Program(emptyList()))
////        val command = AST.SimpleCommand("echo", listOf(AST.WordArgument("hello world")))
////        val output = StringBuilder()
////        executor.simpleCommand(command = command, output = output.asOutputStream())
////        println("Captured Output: ${output.toString().trim()}")
////
////
////    }
////
////    @Test
////    fun stdinManuel() {
////        val executor = Executor(AST.Program(emptyList()))
////        val command = AST.SimpleCommand("cat", emptyList())
////        val input = "hello there".byteInputStream()
////        val output = StringBuilder()
////        executor.simpleCommand(command, input = input, output = output.asOutputStream())
////        println("Captured Output: ${output.toString().trim()}")
////    }
}