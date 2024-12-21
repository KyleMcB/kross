package com.xingpeds.kross

import com.xingpeds.kross.luaScripting.Lua
import kotlinx.coroutines.runBlocking
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.File

class CommandCompleter(val lua: Lua) : Completer {
    private val commands: List<String> by lazy {
        listExecutablesOnPath()
    }

    //Candidate(String value, String displ, String group, String descr, String suffix, String key, boolean complete)
    override fun complete(p0: LineReader?, p1: ParsedLine?, list: MutableList<Candidate>?) {
        val candidates = commands.map { command ->
            Candidate(
                command,
                command,
                "Executable",
                null,
                null,
                null,
                true
            )
        }
        val luaCandidates = runBlocking {
            lua.userFuncs().map { command ->
                Candidate(command, command, "Lua Function", null, null, null, true)
            }
        }
        list?.addAll(candidates + luaCandidates)
    }
}

fun listExecutablesOnPath(): List<String> {
    val pathEnv = System.getenv("PATH") ?: return emptyList()
    val pathSeparator = File.pathSeparator
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val windowsExtensions = setOf(".exe", ".bat", ".cmd", ".com")

    return pathEnv.split(pathSeparator)
        .flatMap { dir ->
            val folder = File(dir)
            folder.listFiles()?.toList().orEmpty()
                .filter { file ->
                    file.isFile &&
                            (if (isWindows) {
                                val lower = file.name.lowercase()
                                windowsExtensions.any { lower.endsWith(it) } && file.canExecute()
                            } else {
                                file.canExecute()
                            })
                }
                .map { it.name }
        }
        .distinct()
        .sorted()
}
