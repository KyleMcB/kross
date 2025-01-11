package com.xingpeds.kross.parser

import com.xingpeds.kross.entities.*
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import com.xingpeds.kross.state.UserCommandHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private fun log(any: Any) = println("--ExecutorTest: $any")
class ExecutorTest {
    val tempDir = Files.createTempDirectory("mockCWD").toFile()

    init {
        File(tempDir, "file1.txt").createNewFile()
        File(tempDir, "file2.txt").createNewFile()
        File(tempDir, "notes.md").createNewFile()
        File(tempDir, "logfile.log").createNewFile()
    }

    val processExecutable: (name: String) -> Executable = { _: String -> JavaOSProcess() }
    val cwd = MutableStateFlow(tempDir)
    val mockShellState: ShellState = object : ShellState {
        val _environment = MutableStateFlow<Map<String, String>>(mapOf("world" to "hi"))
        override val currentDirectory: StateFlow<File>
            get() = MutableStateFlow<File>(tempDir)

        override suspend fun changeDirectory(directory: File) {
            TODO("Not yet implemented")
        }

        override val environment: StateFlow<Map<String, String>>
            get() = _environment

        override suspend fun setVariable(name: String, value: String) {
            _environment.update {
                it.toMutableMap().apply { put(name, value) }
            }
        }

        override suspend fun addHistory(command: String) {
            TODO("Not yet implemented")
        }

        override val history: StateFlow<UserCommandHistory>
            get() = TODO("Not yet implemented")

    }

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
            log("closing pipes......")
            pipes.programInput?.close()
            pipes.programOutput?.close()
        }.join()
        assertEquals("hello world", output.toString().trim())
    }

    @Ignore // TODO fix me this test is tied to the actual system environment
    @Test
    fun variableSub() = runTest(timeout = 10.seconds) {
        val shellStateObject = ShellStateObject
        shellStateObject.setVariable("hello", "world")
        println(shellStateObject.environment.value)
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
        val pipe = SupervisorChannel()
        val pipes = Pipes(
            programOutput = pipe
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
            launch {
                executor.execute(ast)
                pipe.superClose()
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

    @Ignore //need to mock out env
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
            programOutput = Chan(),
        )
        val executor = Executor(cwd, processExecutable, pipes = pipes)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                executor.execute(ast)
                pipes.programOutput?.close()
            }
            launch {
                pipes.programOutput?.connectTo(output.asOutputStream())
            }
        }.join()
        println(output)
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
        val pipe = Chan()
        val executor = Executor(cwd, processExecutable, pipes = Pipes(programOutput = pipe))
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                executor.execute(ast)
            }
            launch {
                pipe.connectTo(output.asOutputStream())
            }
        }.join()
        assertEquals(2, output.toString().count { it == ':' })
    }

    @Test
    fun varInQuotesWrapped() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.DoubleQuoteWithVar("hello \${world}")
                            )
                        )
                    )
                )
            )
        )
        val randomWord = (Int.MIN_VALUE..Int.MAX_VALUE).random().toString()
        mockShellState.setVariable("world", randomWord)
        val output = StringBuilder()
        val pipe = Chan()
        val executor = Executor(cwd, processExecutable, Pipes(programOutput = pipe), mockShellState)
        CoroutineScope(Dispatchers.Default).launch {
            launch {

                executor.execute(ast)
            }
            launch {
                pipe.connectTo(output.asOutputStream())
                pipe.close()
            }
        }.join()
        val expected = "hello $randomWord"
        assertEquals(expected, output.toString().trim())
    }

    @Test
    fun varInQuotes() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.DoubleQuoteWithVar("hello \$world")
                            )
                        )
                    )
                )
            )
        )
        val randomWord = (Int.MIN_VALUE..Int.MAX_VALUE).random().toString()
        mockShellState.setVariable("world", randomWord)
        val output = StringBuilder()
        val pipe = Chan()
        val executor = Executor(cwd, processExecutable, Pipes(programOutput = pipe), mockShellState)
        CoroutineScope(Dispatchers.Default).launch {
            launch {

                executor.execute(ast)
            }
            launch {
                pipe.connectTo(output.asOutputStream())
                pipe.close()
            }
        }.join()
        val expected = "hello $randomWord"
        assertEquals(expected, output.toString().trim())
    }

    @Test
    fun globTest() = runTest(timeout = 10.seconds) {
        val ast = AST.Program(
            commands = listOf(
                AST.Command.Pipeline(
                    commands = listOf(
                        AST.SimpleCommand(
                            name = AST.CommandName.Word("echo"),
                            arguments = listOf(
                                AST.Argument.Glob(text = "*.txt")
                            )
                        )
                    )
                )
            )
        )
        val pipe = Chan()
        val executor =
            Executor(
                mockShellState.currentDirectory,
                processExecutable,
                pipes = Pipes(programOutput = pipe),
                shellState = mockShellState
            )
        val output = StringBuilder()
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                executor.execute(ast)
            }
            launch {
                pipe.connectTo(output.asOutputStream())
                pipe.close()
            }
        }.join()
        val outputSet = output.toString().trim().split(" ").toSet()
        assertEquals(setOf("file1.txt", "file2.txt"), outputSet)
    }

}