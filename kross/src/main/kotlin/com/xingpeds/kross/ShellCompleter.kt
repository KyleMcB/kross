package com.xingpeds.kross

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class ShellCompleter(
    private val commandCompleter: CommandCompleter,
    private val currentDirectoryCompleter: CurrentDirectoryCompleter
) : Completer {


    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        //this should get parsed and the completer is choosen based on what the type of token is right now
        val wordIndex = line.wordIndex()
        if (wordIndex == 0) {
            commandCompleter.complete(reader, line, candidates)
        }
        // TODO then send to lua
        else {
            currentDirectoryCompleter.complete(reader, line, candidates)
        }
    }

    // a simple data class for clarity
    data class Command(val name: String, val description: String)

}