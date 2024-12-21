package com.xingpeds.kross

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.File


class CurrentDirectoryCompleter(
    val cwd: StateFlow<File>,
    val scope: CoroutineScope,
) : Completer {
    //Candidate(String value, String displ, String group, String descr, String suffix, String key, boolean complete)
    val candidates = cwd.map { cwd ->
        val directories = cwd.listFiles()?.filter { it.isDirectory }
            ?.map { dir -> Candidate(dir.name, dir.name, "Directory", null, null, null, true) } ?: emptyList()
        val files = cwd.listFiles()?.filter { it.isFile }
            ?.map { file -> Candidate(file.name, file.name, "File", null, null, null, true) } ?: emptyList()
        directories + files
    }.stateIn(scope, started = SharingStarted.Eagerly, initialValue = emptyList())

//    init {
//
//        scope.launch {
//            candidates.collect {
//                println(it)
//            }
//        }
//    }

    override fun complete(p0: LineReader?, p1: ParsedLine?, list: MutableList<Candidate>?) {
        list?.addAll(candidates.value)
    }
}