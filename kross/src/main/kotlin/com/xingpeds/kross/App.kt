/*
 * This source file was generated by the Gradle 'init' task
 */
package com.xingpeds.kross

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.OnKeyPressedScope
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
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
val baseTime = System.currentTimeMillis()

sealed class KeyEvent {
    data class Esc(val time: Long) : KeyEvent()
    data class Character(val char: Char, val time: Long) : KeyEvent()
    data class Alt(val char: Char, val time: Long) : KeyEvent()
}


fun Flow<Int>.processKeys(): Flow<KeyEvent> = flow {
    val timeoutMs = 150L // Adjust based on user input speed

    collect { key ->
        val hi: Long = System.currentTimeMillis()
        if (key == 27) { // Detected Esc
            val nextKey = withTimeoutOrNull(timeoutMs) { firstOrNull() }
            if (nextKey != null) {
                emit(KeyEvent.Alt(nextKey.toChar(), hi - baseTime))
            } else {
                emit(KeyEvent.Esc(hi - baseTime))
            }
        } else {
            emit(KeyEvent.Character(key.toChar(), hi - baseTime))
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
    val bufferState = MutableStateFlow("")
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
        bufferState.emit("")
        var finished = false
        val terminal = SystemTerminal()
        session(terminal = terminal) {

            section {
                if (finished) {
                    val terminalWidth = terminal.width

                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminalWidth - 2) {
                            textLine(promptState.value.dropLast(2))
                            textLine(bufferState.value)
                        }

                    }
                } else {
                    bordered(borderCharacters = BorderCharacters.CURVED) {
                        justified(Justification.LEFT, minWidth = terminal.width - 2) {
                            textLine("${promptState.value} ${bufferState.value}")

                        }

                    }
                }
            }.runUntilSignal {
                onKeyPressed {
                    onKeyPressedKross(this, collectionScope, this@runUntilSignal, bufferState)
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
        if (bufferState.value.isBlank()) continue
        if (bufferState.value.equals("exit", ignoreCase = true)) break
        val time = measureTimeMillis {
            processinput(bufferState.value)
        }
        state.addHistory(bufferState.value)
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