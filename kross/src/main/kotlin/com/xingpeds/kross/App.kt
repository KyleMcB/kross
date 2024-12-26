package com.xingpeds.kross

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.OnKeyPressedScope
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.invert
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotterx.decorations.BorderCharacters
import com.varabyte.kotterx.decorations.bordered
import com.varabyte.kotterx.text.Justification
import com.varabyte.kotterx.text.justified
import com.xingpeds.kross.builtins.BuiltInExecutable
import com.xingpeds.kross.entities.json
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
import kotlinx.serialization.json.encodeToStream
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    username: String
): StateFlow<String> {
    return combine(timeFlow, cwdState) { time, cwdFile ->
        val userhome = System.getProperty("user.home")
        val cwd = cwdFile.absolutePath.replace(userhome, "~")
        "$username $time $cwd> "
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default), // Use appropriate coroutine scope
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

fun CoroutineScope.readUntilEnter(terminal: SystemTerminal, output: Channel<Int>) = launch {

    while (true) {
        try {

            val byte = terminal.read(15)
            if (byte >= 0) {
                output.send(byte)
            }
            when (byte) {
                10 -> {
                    output.close()
                    break
                }

                13 -> {
                    output.close()
                    break
                }

                -1 -> {
                    output.cancel()
                }
            }
        } catch (e: Exception) {
            //bla
        }
    }
}

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    val state: ShellState = ShellStateObject
    ShellStateObject.setHistoryFile(getHistoryFile())
    val lua: Lua = LuaEngine
    val initFile = initFile()
    lua.executeFile(initFile)
    val bufferState = MutableStateFlow<EditState>(EditState("", 0))
    val username = System.getProperty("user.name")
    val promptState = createPromptState(
        timeFlow, state.currentDirectory,
        username = username
    )
    while (true) {
        val collectionScope = CoroutineScope(Dispatchers.Default)

        // Prompt the user and read input
        val userhome: String = System.getProperty("user.home")
        val cwd: String = ShellStateObject.currentDirectory.value.absolutePath.replace(userhome, "~")
        // todo hook up the lua prompt
        var prompt = "$username $cwd> "
        val promptfunc = LuaEngine.global.key("kross")?.key("handles")?.key("prompt")?.funcOrNull()
        if (promptfunc != null) {
            prompt = promptfunc.call().tojstring()
        }
        bufferState.emit(EditState("", 0))
        var finished = false
        val terminal = SystemTerminal()
        session(terminal = terminal) {

            section {
                if (finished) {
                    val terminalWidth = terminal.width

                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminalWidth - 2) {
                            textLine(promptState.value.dropLast(2))
                            textLine(bufferState.value.content)
                        }

                    }
                } else {
                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminal.width - 2) {
//                            textLine("${promptState.value} ${bufferState.value}")
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
//                            textLine(
//                                if (bufferState.value.isBlank()) " " else ""
//                            )
                        }

                    }
                }
            }.runUntilSignal {
                val channel = Channel<Int>(Channel.UNLIMITED)
                val keyFlow = toKeyEventFlow(channel).shareIn(collectionScope, SharingStarted.Eagerly)
                collectionScope.launch {
                    readUntilEnter(terminal, channel)
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
//                collectionScope.launch {
//                    keyFlow.filterIsInstance(KeyEvent.Unknown::class).collect { unknown ->
//                        bufferState.update {
//                            it + unknown.code
//                        }
//                    }
//                }
                collectionScope.launch {
                    keyFlow.filterIsInstance(KeyEvent.CR::class).collect {
                        // for now we stop input mode and process
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
                        finished = true
                        rerender()
                    }.collect {
                        rerender()
                    }
                }
            }
        }
        // end of collection stage. execute the input
        if (bufferState.value.content.isBlank()) continue
        if (bufferState.value.content.equals("exit", ignoreCase = true)) break
        val time = measureTimeMillis {
            processinput(bufferState.value.content)
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

    scope.cancel()
}

private fun onKeyPressedKross(
    onKeyPressedScope: OnKeyPressedScope,
    collectionScope: CoroutineScope,
    runScope: RunScope,
    bufferState: MutableStateFlow<String>
) {
    when (onKeyPressedScope.key) {
        Keys.ENTER -> {
            collectionScope.cancel()
            runScope.signal()
        }


        Keys.BACKSPACE -> {
            if (bufferState.value.isNotBlank()) {
                bufferState.update {
                    it.dropLast(1)
                }
            }
        }

        Keys.ESC -> {}
        Keys.UP -> {}
        Keys.DOWN -> {}
        Keys.LEFT -> {}
        Keys.RIGHT -> {}
        Keys.HOME -> {}
        Keys.END -> {}
        Keys.DELETE -> {}
        Keys.TAB -> {}
        Keys.INSERT -> {}
        Keys.PAGE_UP -> {}
        Keys.PAGE_DOWN -> {}

        else -> bufferState.update {
            it + onKeyPressedScope.key
        }
    }
}

suspend fun processinput(line: String) {

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
        executor.execute(ast)
    } catch (e: Exception) {
        println("failed to run command: ${e.message}")
// this should be in debug mode only
        println(e.stackTraceToString())
    }

}

fun getHistoryFile(): File {
    // Get the path to the history file
    val historyFilePath = "${System.getProperty("user.home")}/.config/kross/data/history.json"
    val historyFile = File(historyFilePath)

    // Ensure the parent directories and the file exist
    if (!historyFile.exists()) {
        historyFile.parentFile.mkdirs() // Create parent directories if they do not exist
        historyFile.createNewFile()    // Create the file if it does not exist
        json.encodeToStream(emptyList<String>(), historyFile.outputStream())
    }

    return historyFile
}

fun getCurrentTime(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return currentTime.format(formatter)
}

fun String.insertAt(index: Int, string: String): String {
    if (index !in 0..length) throw IndexOutOfBoundsException("Index $index out of bounds for length $length")
    return this.substring(0, index) + string + this.substring(index)
}

fun String.dropAt(index: Int): String {
    return when (index) {
        0 -> {
            this
        }

        in 1 until length -> {
            this.substring(0, index - 1) + this.substring(index)
        }

        else -> {
            this.dropLast(1)
        }
    }
}