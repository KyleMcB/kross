package com.xingpeds.kross

import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.render.OffscreenRenderScope
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotterx.decorations.BorderCharacters
import com.varabyte.kotterx.decorations.bordered
import com.varabyte.kotterx.text.Justification
import com.varabyte.kotterx.text.justified
import com.xingpeds.kross.builtins.BuiltInExecutable
import com.xingpeds.kross.entities.*
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.executableLua.LuaExecutable
import com.xingpeds.kross.luaScripting.Lua
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.executeFile
import com.xingpeds.kross.luaScripting.key
import com.xingpeds.kross.parser.*
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

val isWindows = System.getProperty("os.name").lowercase().contains("win")
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

fun createPromptfunc(
    timeFlow: Flow<String>,
    cwdState: StateFlow<File>,
    gitBranch: StateFlow<String?>,
    username: String
): StateFlow<RenderScope.() -> Unit> {
    return combine(timeFlow, cwdState, gitBranch.map { it?.trim() }) { time, cwdFile, branch ->
        val userhome = System.getProperty("user.home")
        val cwd = cwdFile.absolutePath.replace(userhome, "~")
        if (branch?.isNotBlank() == true) {
            val textline: RenderScope.() -> Unit = {
                text("branch: ")
                blue {
                    textLine(branch)
                }
                blue { text("$username ") }
                text(time)
                green { text(" $cwd") }
                textLine(" >")
            }
            textline
        } else {
            {
                blue { text("$username ") }
                text(time)
                green { text(" $cwd") }
                textLine(" >")
            }
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default), // TODO Use appropriate coroutine scope
        started = SharingStarted.Eagerly,
        initialValue = {}
    )
}

val timeFlow = flow {
    while (true) {
        emit(getCurrentTime()) // Emit the current time
        delay(1000) // Wait for 1 second
    }
}

data class EditState(val content: String, val cursor: Int, val tokens: List<Token>)

enum class ProcessStep {
    UserCommand,
    HistorySearch,
    TabComplete,
    TypeInEditor,
    FindFile
}

val keyMap: MutableMap<KeyEvent, suspend () -> Unit> = mutableMapOf()
suspend fun String.toTokens(): List<Token> {
    return try {
        Lexer(this).tokens().toList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    val state: ShellState = ShellStateObject
    ShellStateObject.setHistoryFile(getHistoryFile())
    val lua: Lua = LuaEngine
    val initFile = initFile()
    lua.executeFile(initFile)
    val bufferState = MutableStateFlow<EditState>(EditState("", 0, emptyList()))
    bufferState.map { (content, _) ->
        // how many "words" are in content
        val count = content.count { it == ' ' } + 1
        if (count == 1) {
            listExecutablesOnPath()
        }
    }
    val gitBranch = MutableStateFlow<String?>(null)

    val username = System.getProperty("user.name")

    val promptState = createPromptfunc(
        timeFlow, state.currentDirectory,
        username = username,
        gitBranch = gitBranch,
    )
    val heldOverOuput = MutableStateFlow("")
    while (true) {
        val restartListeningSignal = MutableSharedFlow<Unit>()
        val restartListening = suspend {
            restartListeningSignal.emit(Unit)
        }
        val handler = CoroutineExceptionHandler { _, exception ->
            exception.error("uncaught error")
        }
        val collectionScope = CoroutineScope(Dispatchers.Default + handler)
        collectionScope.gitBranch(gitBranch)
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
        val startingBuffer = EditState(heldOutput, heldOutput.length, heldOutput.toTokens())
        bufferState.emit(startingBuffer)
        heldOverOuput.emit("")
        // TODO finished could be replaced by a nullable processStep state
        val finished = MutableStateFlow(false)
        val terminal = SystemTerminal() {
            if (bufferState.value.content.isBlank()) {
                exitProcess(0)
            } else {
                runBlocking {
                    bufferState.emit(EditState("", 0, emptyList()))
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
                                    val promptFunc = promptState.value
                                    promptFunc()
                                    printColorized(bufferState)
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

                        ProcessStep.FindFile -> {
                            // leave nothing behind, this will make the input box looks continuous
                            // FIXME I don't know what there is a line printed when the section is left
                            // for now I am manually moving the cursor up one line to compensate
                            terminal.write(Codes.Keys.UP.toFullEscapeCode())
                        }
                    }
                } else {
                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminal.width - 2) {
//                            text(promptState.value)
                            val promptfunc = promptState.value
                            promptfunc()
                            printBufferWithInvert(bufferState)
                        }
                    }
                }
            }.runUntilSignal {

                val channel = Channel<Int>(Channel.UNLIMITED)
                val keyFlow = toKeyEventFlow(channel).shareIn(collectionScope, SharingStarted.Eagerly)
                collectionScope.launch {
                    readUntil(channel) {
                        terminal.read(15)
                    }
                }
                collectionScope.launch {
                    restartListeningSignal.collect {
                        readUntil(channel) {
                            terminal.read(15)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.collect { keyEvent ->

                        when (keyEvent) {
                            is KeyEvent.Alt, is KeyEvent.Ctrl, KeyEvent.Tab -> {

                                if (keyMap.containsKey(keyEvent)) {
                                    keyMap[keyEvent]?.invoke()
                                } else {
                                    restartListening()
                                }
                            }

                            else -> Unit
                        }
                    }
                }
                // this looks like a memory leak
                keyMap[KeyEvent.Ctrl('F')] = {
                    finished.emit(true)
                    processState.emit(ProcessStep.FindFile)
                    rerender()
                    signal()
                    collectionScope.cancel()
                }
                keyMap[KeyEvent.Ctrl('O')] = {
                    finished.emit(true)
                    processState.emit(ProcessStep.TypeInEditor)
                    rerender()
                    signal()
                    collectionScope.cancel()
                }
                keyMap[KeyEvent.Alt("a")] = {
                    bufferState.update { (content, cursor, tokens) ->
                        EditState(content, 0, tokens)
                    }
                    restartListening()
                }
                keyMap[KeyEvent.Alt("A")] = {
                    bufferState.update { (content, cursor, tokens) ->
                        EditState(content, content.length, tokens)
                    }
                    restartListening()
                }
                keyMap[KeyEvent.Ctrl('R')] = {
                    processState.emit(ProcessStep.HistorySearch)
                    finished.emit(true)
                    rerender()
                    signal()
                    collectionScope.cancel()
                }
                keyMap[KeyEvent.Tab] = {
                    finished.emit(true)
                    processState.emit(ProcessStep.TabComplete)
                    rerender()
                    signal()
                    collectionScope.cancel()
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.UpArrow::class).collect {
                        historyCursor = min(historyCursor?.plus(1) ?: 0, state.history.value.lastIndex)
                        val content = state.history.value[historyCursor!!].first
                        bufferState.emit(
                            EditState(content, content.length, content.toTokens())
                        )
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.DownArrow::class).collect {
                        historyCursor = max(historyCursor?.minus(1) ?: 0, 0)
                        val content = state.history.value[historyCursor!!].first
                        bufferState.emit(
                            EditState(content, content.length, content.toTokens())
                        )
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.LeftArrow::class).collect {
                        bufferState.update { (content, cursor, tokens) ->
                            EditState(content, if (cursor > 0) cursor - 1 else 0, tokens)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.RightArrow::class).collect {
                        bufferState.update { (content, cursor, tokens) ->
                            EditState(content, if (cursor < content.length) cursor + 1 else content.length, tokens)
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.Character::class).collect { charEvent ->
                        bufferState.update { (content, cursor) ->
                            val content1 = content.insertAt(cursor, charEvent.text)
                            EditState(content1, cursor + charEvent.text.length, content1.toTokens())
                        }
                    }
                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.Backspace::class).collect { backspaceEvent ->
                        bufferState.update { (content, cursor) ->
                            val content1 = content.dropAt(cursor)
                            EditState(
                                content1,
                                if (cursor > 0) cursor - 1 else 0,
                                content1.toTokens()
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
                    promptState.collect {
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
                            args = listOf("--height=~50%", "--query=${bufferState.value.content}", "-1"),
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
                    if (isWindows) {
                        listExecutablesOnPath()
                            .map { it.substringBeforeLast(".") }
                            .joinToString(separator = "\n") { it }
                    } else
                        listExecutablesOnPath().joinToString(separator = "\n") { it }
                } else state.currentDirectory.value.list()?.joinToString(separator = "\n") { it }
                val inputPipe = if (candidates != null) {
                    Channel<Int>(Channel.UNLIMITED)
                } else null
                val pipes = Pipes(programInput = inputPipe, programOutput = outputPipe)
                var fzfExitCode = -99
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
                            listOf("--height=~50", "--query=$lastWord", "-1")
                        } else emptyList()
                        fzfExitCode = executor.invoke(
                            "fzf",
                            args = args,
                            pipes = pipes,
                            env = state.environment.value,
                            cwd = state.currentDirectory.value
                        )()
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
                } else if (fzfExitCode != 0) {
                    heldOverOuput.emit(bufferState.value.content)
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

            ProcessStep.FindFile -> {

                val fzfScope = CoroutineScope(Dispatchers.Default)
                val outputPipe = Channel<Int>(Channel.UNLIMITED)
                val output = StringBuilder()
                val buffer = bufferState.value.content
                val words = buffer.split(" ")
                val pipes = Pipes(programOutput = outputPipe)
                fzfScope.launch {
                    launch {
                        outputPipe.connectTo(output.asOutputStream())
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
        }
    }

    scope.cancel()
}

val colorMap: Map<TokenType, Int?> = TokenType.entries.associate {
    when (it) {
        TokenType.Word -> it to null
        TokenType.Semicolon -> it to 0xFFFF00
        TokenType.Pipe -> it to 0xFFFF00
        TokenType.And -> it to 0xFFFF00
        TokenType.Or -> it to 0xFFFF00
        TokenType.LeftParen -> it to 0xFFFF00
        TokenType.RightParen -> it to 0xFFFF00
        TokenType.SingleQuotedString -> it to 0xFFFF00
        TokenType.DoubleQuotedString -> it to 0xFFFF00
        TokenType.Dollar -> it to 0xFFFF00
        TokenType.EOF -> it to null
        TokenType.WordWithGlob -> it to 0xFFFF00
        TokenType.DoubleQuotedStringWithEnv -> it to 0xFFFF00
        TokenType.WordWithDoubleGlob -> it to 0xFFFF00
    }
}

private fun OffscreenRenderScope.printBufferWithInvert(bufferState: StateFlow<EditState>) {
    val bufferSnapShot = bufferState.value.content
    val cursorIndex = bufferState.value.cursor
    val tokens = bufferState.value.tokens
    for ((index, c) in bufferSnapShot.toCharArray().withIndex()) {
        val token = tokens.find { index in it.position }
        val color: Int? = token?.type?.let { colorMap[it] }
        if (index == cursorIndex) {
            invert {
                if (color != null) {
                    rgb(color) {
                        text(c)
                    }
                } else {
                    text(c)
                }
                // color for token lookup
            }
        } else {
            if (color != null) {
                rgb(color) {
                    text(c)
                }
            } else {
                text(c)
            }
        }
    }
    if (bufferSnapShot.length == cursorIndex) {
        invert {
            text(" ")
        }
    }
}

private fun OffscreenRenderScope.printColorized(bufferState: StateFlow<EditState>) {
    val (content, cursor, tokens) = bufferState.value

    var index = 0
    while (index < content.length) {
        val token = tokens.find { index in it.position }
        if (token != null) {
            when (token) {
                is Token.And -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.Dollar -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.EOF -> Unit

                is Token.LeftParen -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.DoubleQuote -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.SingleQuote -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.Word -> {
                    val text = content.substring(token.position)
                    text(text)
                    index += text.length
                }

                is Token.Or -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.Pipe -> {
                    val text = content.substring(token.position)
                    blue {
                        text(text)
                    }
                    index += text.length
                }

                is Token.RightParen -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.Semicolon -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.DoubleQuoteWithVar -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.Glob -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }

                is Token.RecursiveGlob -> {
                    val text = content.substring(token.position)
                    green {
                        text(text)
                    }
                    index += text.length
                }
            }
        } else {
            text(content[index])
            index += 1
        }
    }
}

suspend fun processInput(line: String) {

    try {

        val state: ShellState = ShellStateObject
        val lexer = Lexer(line)
        val parser = Parser()
        val ast = parser.parse(lexer.tokens())
        val makeExecutable: suspend (name: String) -> Executable = { name ->

            // Check if 'name' is a valid executable file
            val executableFile = File(state.currentDirectory.value, name)
            if (executableFile.exists() && executableFile.canExecute() && executableFile.isFile) {
                JavaOSProcess()
            } else if (LuaEngine.userFuncExists(name)) {
                LuaExecutable()
            } else if (Builtin.builtinFuns.containsKey(name)) {
                BuiltInExecutable(Builtin.builtinFuns[name]!!)
            } else {
                val programs = listExecutablesOnPath()
                if (programs.contains(name)) {
                    JavaOSProcess()
                } else {
                    val matchedProgram = programs.find { program ->
                        val programWithoutExtension = program.substringBeforeLast('.')
                        programWithoutExtension.equals(name, ignoreCase = true)
                    }

                    if (matchedProgram != null) {
                        // Use the full program name with extension as overrideName
                        JavaOSProcess(overrideName = matchedProgram)
                    } else {
                        throw Exception("Program '$name' not found on PATH.")
                    }
                }
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

