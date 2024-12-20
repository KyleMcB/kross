package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.*
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private fun log(any: Any) = println("--ExecutorTest: $any")
class ExecutorTest {

    val processExecutable: (name: String) -> Executable = { _: String -> JavaOSProcess() }
    val cwd = MutableStateFlow(File(System.getProperty("user.dir")))

    @Test
    fun simpleEcho() = runTest(timeout = 10.seconds) {
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
        val output = StringBuilder()
        val pipes = Pipes(
            programOutput = Chan()
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                log("starting pipe")
                pipes.programOutput?.connectTo(output.asOutputStream())
                log("finished pipe")
            }
            launch {
                log("executing ast")
                executor.execute(ast)
                pipes.programOutput?.close()
                log("finished executing ast")
            }
        }.join()
        assertEquals("hello world", output.toString().trim())
    }

    @Test
    fun simpleCat() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    listOf(
                        AST.SimpleCommand(
                            AST.CommandName.Word("cat"),
                        )
                    )
                )
            )
        )
        val output = StringBuilder()
        val input = "hello world".byteInputStream()
        val pipes = Pipes(
            programInput = Chan(),
            programOutput = Chan()
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                pipes.programInput?.connectTo(input)
            }
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            executor.execute(ast)
            pipes.programInput?.close()
            pipes.programOutput?.close()
        }.join()
        assertEquals("hello world", output.toString().trim())
    }

    @Test
    fun variableSub() = runTest(timeout = 10.seconds) {
        ShellStateObject.setVariable("hello", "world")
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
        val output = StringBuilder()
        val pipes = Pipes(
            programOutput = Chan()
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            executor.execute(ast)
            pipes.programOutput?.close()
        }.join()
        assertEquals("world", output.toString().trim())
    }

    @Test
    fun seq1() = runTest(timeout = 10.seconds) {
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
        val pipes = Pipes(
            programOutput = Chan()
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            launch {
                executor.execute(ast)
                pipes.programOutput?.close()
            }
        }.join()
        assertEquals("hello\nworld\n", output.toString())
    }

    @Test
    fun and2() = runTest(timeout = 10.seconds) {
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
        val executor = Executor(cwd, processExecutable)
        val returnCodes = executor.execute(ast)
        assertEquals(0, returnCodes[0])
        assertEquals(1, returnCodes[1])
    }

    @Test
    fun and1() = runTest(timeout = 10.seconds) {
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
        val executor = Executor(cwd, processExecutable)
        val returnCodes = executor.execute(ast)
        assertEquals(0, returnCodes[0])
        assertEquals(0, returnCodes[1])
    }

    @Test
    fun and3() = runTest(timeout = 10.seconds) {
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

        val executor = Executor(cwd, processExecutable)
        val returnCodes = executor.execute(ast)
        assertEquals(1, returnCodes.size)
        assertEquals(1, returnCodes[0])
    }

    @Test
    fun or1() = runTest(timeout = 10.seconds) {

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
        val executor = Executor(cwd, processExecutable)
        val returnCodes = executor.execute(ast)
        assertEquals(1, returnCodes.size)
        assertEquals(0, returnCodes[0])
    }

    @Test
    fun or2() = runTest(timeout = 10.seconds) {

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
        val executor = Executor(cwd, processExecutable)
        val returnCodes = executor.execute(ast)
        assertEquals(2, returnCodes.size)
        assertEquals(1, returnCodes[0])
        assertEquals(0, returnCodes[1])
    }


    @Test
    fun pipe2() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(AST.Argument.WordArgument("hello there"))
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("cat"),
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("cat")
                        )
                    )
                )
            )
        )
        val output = StringBuilder()
        val pipes = Pipes(
            programOutput = Chan(),
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            launch {

                val results = executor.execute(ast)
                assertEquals(listOf(0, 0, 0), results)
            }
        }.join()
        assertEquals("hello there", output.toString().trim())
    }

    @Test
    fun pipe1() = runTest(timeout = 10.seconds) {
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
        val pipes = Pipes(
            programOutput = Chan(),
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                executor.execute(ast)
            }
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
                pipes.programOutput?.close()
            }
        }.join()
        assertEquals("hello there", output.toString().trim())
    }

    @Test
    fun grepChan() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("ls"),
                        ),
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("grep"),
                            arguments = listOf(
                                AST.Argument.WordArgument("build"),
                            )
                        )
                    )
                )
            )
        )
        val output = StringBuilder()
        val pipes = Pipes(
//            programOutput = Chan(),
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                executor.execute(ast)
            }
        }
    }

    @Test
    fun commandSub() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.CommandSubstitution(
                                    AST.Program(
                                        commands = listOf(
                                            AST.Command.Pipeline(
                                                commands = listOf(
                                                    AST.SimpleCommand(
                                                        name = AST.CommandName.Word("date")
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
//        val executor = executorCwd()
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
//            executor.execute(ast, streams = streams)
        }.join()
        // Note: Adjust expectedOutput according to the date format implementation
//        val expectedOutput = /* Adjust expected output here if applicable */
//        assertEquals(expectedOutput, output.toString().trim())
        println(output.toString().trim())

    }

}