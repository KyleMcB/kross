/*
 * This source file was generated by the Gradle 'init' task
 */
package com.xingpeds.kross

import com.github.ajalt.mordant.terminal.Terminal
import com.xingpeds.kross.builtins.BuiltInExecutable
import com.xingpeds.kross.entities.json
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.executableLua.LuaExecutable
import com.xingpeds.kross.luaScripting.Lua
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.executeFile
import com.xingpeds.kross.parser.Executor
import com.xingpeds.kross.parser.Lexer
import com.xingpeds.kross.parser.Parser
import com.xingpeds.kross.state.Builtin
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.encodeToStream
import java.io.File


fun main() = runBlocking {
    val state: ShellState = ShellStateObject
    state.setHistoryFile(getHistoryFile())
    val lua: Lua = LuaEngine
    val terminal = Terminal()
    val initFile = initFile()
    lua.executeFile(initFile)

    generateSequence {
        print("input ")
        terminal.readLineOrNull(false)
    }
        .filterNotNull()
        .filter { it.isNotBlank() }
        .takeWhile { it != "exit" }
        .forEach {

            state.addHistory(it)
            try {

                val lexer = Lexer(it)
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
}

fun getHistoryFile(): File {
    // Get the path to the history file
    val historyFilePath = "${System.getProperty("user.home")}/.config/kross/data/history.json"
    val historyFile = File(historyFilePath)

    // Ensure the parent directories and the file exist
    if (!historyFile.exists()) {
        historyFile.parentFile.mkdirs() // Create parent directories if they do not exist
        historyFile.createNewFile()    // Create the file if it does not exist
        json.encodeToStream(
            ListSerializer(String.serializer()),
            listOf(),
            historyFile.outputStream()
        )
    }

    return historyFile
}