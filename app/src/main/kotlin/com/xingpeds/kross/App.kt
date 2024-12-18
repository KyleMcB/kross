/*
 * This source file was generated by the Gradle 'init' task
 */
package com.xingpeds.kross

import com.github.ajalt.mordant.terminal.Terminal
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.executableLua.LuaExecutable
import com.xingpeds.kross.luaScripting.Lua
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.executeFile
import com.xingpeds.kross.parser.BuiltinCommand
import com.xingpeds.kross.parser.Executor
import com.xingpeds.kross.parser.Lexer
import com.xingpeds.kross.parser.Parser
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.runBlocking
import java.io.File


fun main() = runBlocking {
    val state: ShellState = ShellStateObject
    val lua: Lua = LuaEngine
    val terminal = Terminal()
    val initFile = initFile()
    lua.executeFile(initFile)
    val set: BuiltinCommand = { args ->
        if (args.size == 2) {
            state.setVariable(args[0], args[1])
        } else {
            throw Exception("expected two arguments.")
        }
        0
    }
    val cd: BuiltinCommand = { args ->
        // lets assume we get one arg and that is the path to cd to
        val arg = args.firstOrNull()
        if (arg != null) {
            // lets assume its relative for now
            val new = File(state.currentDirectory.value, arg)
            if (new.exists() && new.isDirectory) {
                state.changeDirectory(new)
            }
        } else {
            //cd to home directory
            val home = File(System.getProperty("user.home"))
            state.changeDirectory(home)

        }
        0

    }
    val builtinCommands = mapOf("cd" to cd, "set" to set)

    generateSequence {
        print("input ")
        terminal.readLineOrNull(false)
    }
        .filterNotNull()
        .filter { it.isNotBlank() }
        .takeWhile { it != "exit" }
        .forEach {

            try {

                val lexer = Lexer(it)
                val parser = Parser()
                val ast = parser.parse(lexer.tokens())
                val makeExecutable: suspend (name: String) -> Executable = { name ->
                    if (LuaEngine.userFuncExists(name)) {
                        LuaExecutable()
                    } else {
                        JavaOSProcess()
                    }
                }
                val executor = Executor(cwd = state.currentDirectory, makeExecutable = makeExecutable)
                executor.execute(ast)
            } catch (e: Exception) {
                println("failed to run command: ${e.message}")
                println(e.stackTraceToString())
            }
        }
}
