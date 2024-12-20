package com.xingpeds.kross.state


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
    val builtinFuns: Map<String, BuiltinFun> = mapOf(
        "cd" to cd,
        "setenv" to setenv,
    )
}