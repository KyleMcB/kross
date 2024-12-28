package com.xingpeds.kross

import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.invert
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotterx.decorations.BorderCharacters
import com.varabyte.kotterx.decorations.bordered
import com.varabyte.kotterx.text.Justification
import com.varabyte.kotterx.text.justified
import com.xingpeds.kross.builtins.BuiltInExecutable
import com.xingpeds.kross.entities.Log
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asOutputStream
import com.xingpeds.kross.entities.connectTo
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.executableLua.LuaExecutable
import com.xingpeds.kross.luaScripting.Lua
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.executeFile
import com.xingpeds.kross.luaScripting.key
import com.xingpeds.kross.parser.Executor
import com.xingpeds.kross.parser.Lexer
import com.xingpeds.kross.parser.Parser
import com.xingpeds.kross.state.Builtin
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import java.io.File
import java.nio.file.Files
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun LuaValue.funcOrNull(): LuaFunction? = try {
    this.checkfunction()
} catch (e: Exception) {
    null
}

fun LuaValue.toNullable(): LuaValue? {
    return if (this.isnil()) null else this
}

fun createPromptState(
    timeFlow: Flow<String>,
    cwdState: StateFlow<File>,
    gitBranch: StateFlow<String?>,
    username: String
): StateFlow<String> {
    return combine(timeFlow, cwdState, gitBranch.map { it?.trim() }) { time, cwdFile, branch ->
        val userhome = System.getProperty("user.home")
        val cwd = cwdFile.absolutePath.replace(userhome, "~")
        if (branch.isNullOrBlank().not()) {
            "$branch\n$username $time $cwd> "
        } else {
            "$username $time $cwd> "
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default), // TODO Use appropriate coroutine scope
        started = SharingStarted.Eagerly,
        initialValue = ""
    )
}

val timeFlow = flow {
    while (true) {
        emit(getCurrentTime()) // Emit the current time
        delay(1000) // Wait for 1 second
    }
}

data class EditState(val content: String, val cursor: Int)

fun CoroutineScope.readUntilEnter(
    terminal: SystemTerminal,
    output: Channel<Int>,
    terminalInput: List<Int> = listOf(10, 13)
) = launch {

    while (true) {
        try {

            val byte = terminal.read(15)
            if (byte >= 0) {
                output.send(byte)
            }
            when (byte) {
//                10 -> {
//                    output.close()
//                    break
//                }
//
//                13 -> {
//                    output.close()
//                    break
//                }
                in terminalInput -> {
                    output.close()
                    break
                }

                -1 -> {
                    output.cancel()
                    break
                }
            }
        } catch (e: Exception) {
            //bla
        }
    }
}

enum class ProcessStep {
    UserCommand,
    HistorySearch,
    TabComplete,
    TypeInEditor
}

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    val state: ShellState = ShellStateObject
    ShellStateObject.setHistoryFile(getHistoryFile())
    val lua: Lua = LuaEngine
    val initFile = initFile()
    lua.executeFile(initFile)
    val bufferState = MutableStateFlow<EditState>(EditState("", 0))
    val completions = bufferState.map { (content, cursor) ->
        // how many "words" are in content
        val count = content.count { it == ' ' } + 1
        if (count == 1) {
            listExecutablesOnPath()
        }
    }
    val gitBranch = MutableStateFlow<String?>(null)

    val username = System.getProperty("user.name")
    val promptState = createPromptState(
        timeFlow, state.currentDirectory,
        username = username,
        gitBranch = gitBranch,
    )
    val heldOverOuput = MutableStateFlow("")
    while (true) {
        val collectionScope = CoroutineScope(Dispatchers.Default)
        scope.gitBranch(gitBranch)
        val processState = MutableStateFlow(ProcessStep.UserCommand)
        var historyCursor: Int? = null
        // Prompt the user and read input
        val userhome: String = System.getProperty("user.home")
        val cwd: String = ShellStateObject.currentDirectory.value.absolutePath.replace(userhome, "~")
        // todo hook up the lua prompt
        var prompt = "$username $cwd> "
        val promptfunc = LuaEngine.global.key("kross")?.key("handles")?.key("prompt")?.funcOrNull()
        if (promptfunc != null) {
            prompt = promptfunc.call().tojstring()
        }
        val heldOutput = heldOverOuput.value
        val startingBuffer = EditState(heldOutput, heldOutput.length)
        bufferState.emit(startingBuffer)
        heldOverOuput.emit("")
        // TODO finished could be replaced by a nullable processStep state
        val finished = MutableStateFlow(false)
        val terminal = SystemTerminal() {
            if (bufferState.value.content.isBlank()) {
                exitProcess(0)
            } else {
                runBlocking {
                    bufferState.emit(EditState("", 0))
                }
            }
        }
        session(terminal = terminal) {

            section {
                if (finished.value) {
                    val terminalWidth = terminal.width

                    when (processState.value) {
                        ProcessStep.UserCommand -> {
                            bordered(borderCharacters = BorderCharacters.CURVED) {
                                justified(Justification.LEFT, minWidth = terminalWidth - 2) {
                                    textLine(promptState.value.dropLast(2))
                                    textLine(bufferState.value.content)
                                }

                            }
                        }

                        ProcessStep.HistorySearch -> {
                            // leave nothing behind, this will make the input box looks continuous
                            // FIXME I don't know what there is a line printed when the section is left
                            // for now I am manually moving the cursor up one line to compensate
                            terminal.write(Codes.Keys.UP.toFullEscapeCode())
                        }

                        ProcessStep.TabComplete -> {
                            // leave nothing behind, this will make the input box looks continuous
                            // FIXME I don't know what there is a line printed when the section is left
                            // for now I am manually moving the cursor up one line to compensate
                            terminal.write(Codes.Keys.UP.toFullEscapeCode())
                        }

                        ProcessStep.TypeInEditor -> {
                            // leave nothing behind, this will make the input box looks continuous
                            // FIXME I don't know what there is a line printed when the section is left
                            // for now I am manually moving the cursor up one line to compensate
                            terminal.write(Codes.Keys.UP.toFullEscapeCode())
                        }
                    }
                } else {
                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminal.width - 2) {
                            text(promptState.value)
                            val bufferSnapShot = bufferState.value.content
                            val cursorIndex = bufferState.value.cursor
                            for ((index, c) in bufferSnapShot.toCharArray().withIndex()) {
                                if (index == cursorIndex) {
                                    invert {
                                        text(c)
                                    }
                                } else {
                                    text(c)
                                }
                            }
                            if (bufferSnapShot.length == cursorIndex) {
                                invert {
                                    text(" ")
                                }
                            }
                        }
                    }
                }
            }.runUntilSignal {

                val channel = Channel<Int>(Channel.UNLIMITED)
                val keyFlow = toKeyEventFlow(channel).shareIn(collectionScope, SharingStarted.Eagerly)
                collectionScope.launch {
                    // TODO improve the terminal character list
                    // alt combo are not even possible with this system
                    readUntilEnter(terminal, channel, listOf(10, 13, 18, 9, 15))
                }
                collectionScope.launch {

                    keyFlow.filter { it == KeyEvent.Ctrl('O') }.collect {
                        finished.emit(true)
                        processState.emit(ProcessStep.TypeInEditor)
                        rerender()
                        signal()
                        collectionScope.cancel()
                    }
                }
                collectionScope.launch {
                    keyFlow.filter { it == KeyEvent.Ctrl('R') }.collect {
                        // ctrl-r needs to be terminal like enter
                        processState.emit(ProcessStep.HistorySearch)
                        finished.emit(true)
                        rerender()
                        signal()
                        collectionScope.cancel()
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.Tab::class).collect {
                        // process state to tab complete
                        finished.emit(true)
                        processState.emit(ProcessStep.TabComplete)
                        rerender()
                        signal()
                        collectionScope.cancel()
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.UpArrow::class).collect {
                        historyCursor = min(historyCursor?.plus(1) ?: 0, state.history.value.lastIndex)
                        val content = state.history.value[historyCursor!!].first
                        bufferState.emit(
                            EditState(content, content.length)
                        )
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.DownArrow::class).collect {
                        historyCursor = max(historyCursor?.minus(1) ?: 0, 0)
                        val content = state.history.value[historyCursor!!].first
                        bufferState.emit(
                            EditState(content, content.length)
                        )
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.LeftArrow::class).collect {
                        bufferState.update { (content, cursor) ->
                            EditState(content, if (cursor > 0) cursor - 1 else 0)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.RightArrow::class).collect {
                        bufferState.update { (content, cursor) ->
                            EditState(content, if (cursor < content.length) cursor + 1 else content.length)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.Character::class).collect { charEvent ->
                        bufferState.update { (content, cursor) ->
                            EditState(content.insertAt(cursor, charEvent.text), cursor + charEvent.text.length)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.Backspace::class).collect { backspaceEvent ->
                        bufferState.update { (content, cursor) ->
                            EditState(
                                content.dropAt(cursor),
                                if (cursor > 0) cursor - 1 else 0
                            )
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.CR::class).collect {
                        // for now we stop input mode and process
                        finished.emit(true)
                        rerender()
                        signal()
                        collectionScope.cancel()
                    }
                }
                collectionScope.launch {
                    bufferState.collect {
                        rerender()
                    }
                }
                collectionScope.launch {
                    promptState.onCompletion {
                        finished.emit(true)
                        rerender()
                    }.collect {
                        rerender()
                    }
                }
            }
        }
        terminal.close()
        // end of collection stage. execute the input
        when (processState.value) {
            ProcessStep.UserCommand -> {

                if (bufferState.value.content.isBlank()) continue
                if (bufferState.value.content.equals("exit", ignoreCase = true)) break
                val time = measureTimeMillis {
                    processInput(bufferState.value.content)
                }
                state.addHistory(bufferState.value.content)
                val readableTime =
                    time.toDuration(DurationUnit.MILLISECONDS).toComponents { hours, minutes, seconds, nanoseconds ->
                        buildString {
                            if (hours > 0) append("$hours hours, ")
                            if (minutes > 0 || hours > 0) append("$minutes minutes, ")
                            append("$seconds seconds")
                            if (hours == 0L && minutes == 0) append(", ${nanoseconds / 1_000_000} milliseconds")
                        }
                    }

                println(readableTime)
            }

            ProcessStep.HistorySearch -> {
                val fzfScope = CoroutineScope(Dispatchers.Default)
                val inputPipe = Channel<Int>(Channel.UNLIMITED)
                val outputPipe = Channel<Int>(Channel.UNLIMITED)
                val output = StringBuilder()
                val pipes = Pipes(programInput = inputPipe, programOutput = outputPipe)
                val history = state.history.value.joinToString("\n") { it.first }
                fzfScope.launch {
                    launch {
                        outputPipe.connectTo(output.asOutputStream())
                    }
                    launch {
                        inputPipe.connectTo(history.byteInputStream())
                    }
                    launch {
                        val executor = JavaOSProcess()
                        executor.invoke(
                            "fzf",
                            args = listOf("--query=${bufferState.value.content}", "-1"),
                            pipes = pipes,
                            env = state.environment.value,
                            cwd = state.currentDirectory.value
                        )
                        inputPipe.close()
                        outputPipe.close()
                    }

                }.join()
                heldOverOuput.emit(output.toString().trim())
            }

            ProcessStep.TabComplete -> {

                val fzfScope = CoroutineScope(Dispatchers.Default)
                val outputPipe = Channel<Int>(Channel.UNLIMITED)
                val output = StringBuilder()
                val buffer = bufferState.value.content
                val words = buffer.split(" ")
                val candidates = if (words.size == 1) {
                    listExecutablesOnPath().joinToString(separator = "\n") { it }
                } else state.currentDirectory.value.list()?.joinToString(separator = "\n") { it }
                val inputPipe = if (candidates != null) {
                    Channel<Int>(Channel.UNLIMITED)
                } else null
                val pipes = Pipes(programInput = inputPipe, programOutput = outputPipe)
                fzfScope.launch {
                    launch {
                        outputPipe.connectTo(output.asOutputStream())
                    }
                    launch {
                        candidates?.let { inputPipe?.connectTo(it.byteInputStream()) }
                    }
                    launch {
                        val executor = JavaOSProcess()
                        val lastWord = words.lastOrNull()
                        val args = if (lastWord != null) {
                            listOf("--query=$lastWord", "-1")
                        } else emptyList()
                        executor.invoke(
                            "fzf",
                            args = args,
                            pipes = pipes,
                            env = state.environment.value,
                            cwd = state.currentDirectory.value
                        )
                        inputPipe?.close()
                        outputPipe.close()
                    }

                }.join()
                // now I need to merge the buffer with the output
                val outputstring = output.toString().trim()
                if (outputstring.isBlank().not()) {
                    if (words.size == 1) {
                        heldOverOuput.emit(outputstring)
                    } else {
                        heldOverOuput.emit(
                            words.dropLast(1).joinToString(separator = " ") + " " + outputstring
                        )
                    }
                }
            }

            ProcessStep.TypeInEditor -> {
                //create a temp file
                // Get the EDITOR environment variable or fallback to 'vi'
                val editor = state.environment.value["EDITOR"] ?: "vi"

                // Create a temporary file
                val tempFile = Files.createTempFile("kross-editor-", ".tmp").toFile()
                tempFile.writeText(bufferState.value.content)
                tempFile.deleteOnExit() // Ensure the file gets deleted when the program exits

                val userInput = try {
                    // Open the editor pointing to the temporary file
                    val process = ProcessBuilder(editor, tempFile.absolutePath)
                        .inheritIO() // Use inheritIO to allow the editor to take over the terminal
                        .start()

                    // Wait for the editor process to finish
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        println("Editor exited with error code $exitCode")
                        null
                    }

                    // Read the content of the temporary file
                    tempFile.readText().trim().takeIf { it.isNotEmpty() }
                } catch (e: Exception) {
                    println("Failed to open editor: ${e.message}")
                    null
                } finally {
                    // Clean up the temporary file
                    tempFile.delete()
                }
                heldOverOuput.emit(userInput ?: bufferState.value.content)
            }
        }
    }

    scope.cancel()
}

suspend fun processInput(line: String) {

    try {

        val state: ShellState = ShellStateObject
        val lexer = Lexer(line)
        val parser = Parser()
        val ast = parser.parse(lexer.tokens())
        val makeExecutable: suspend (name: String) -> Executable = { name ->
            if (LuaEngine.userFuncExists(name)) {
                LuaExecutable()
            } else if (Builtin.builtinFuns.containsKey(name)) {
                BuiltInExecutable(Builtin.builtinFuns[name]!!)
            } else {
                JavaOSProcess()
            }
        }
        val executor = Executor(cwd = state.currentDirectory, makeExecutable = makeExecutable)
        val returnCodes = executor.execute(ast)
        println("return codes: $returnCodes")
    } catch (e: Exception) {
        println("failed to run command: ${e.message}")
        Log.error(e)
    }

}

fun getCurrentTime(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return currentTime.format(formatter)

}

