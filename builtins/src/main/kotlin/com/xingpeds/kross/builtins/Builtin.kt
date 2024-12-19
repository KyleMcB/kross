package com.xingpeds.kross.builtins

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipes
import com.xingpeds.kross.state.ShellStateObject
import java.io.File

typealias BuiltinFun = suspend (args: List<String>) -> Int

object Builtin {

    val cd: BuiltinFun = { args: List<String> ->
        try {
            val cwd = ShellStateObject.currentDirectory.value
            val path = args.firstOrNull() ?: System.getProperty("user.home")
            val file = if (File(path).isAbsolute) File(path) else File(cwd, path).canonicalFile
            if (file.exists() && file.isDirectory) {
                ShellStateObject.changeDirectory(file)
                0
            } else {
                println("cd: no such directory: $path")
                -1
            }
        } catch (e: Exception) {
            println("cd: error: ${e.message}")
            -1
        }
    }
    val setenv: BuiltinFun = { args: List<String> ->
        try {
            val name = args.first()
            val value = args.getOrNull(1) ?: ""
            ShellStateObject.setVariable(name, value)
            0
        } catch (e: Exception) {
            -1
        }
    }
    val builtinCommands: Map<String, BuiltInExecutable> = mapOf(
        "cd" to BuiltInExecutable(cd),
        "setenv" to BuiltInExecutable(setenv),
    )
}

class BuiltInExecutable(private val builtin: BuiltinFun) : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult {
        return {
            this@BuiltInExecutable.builtin(args)
        }
    }

}