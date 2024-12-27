package com.xingpeds.kross

import com.xingpeds.kross.entities.getKrossHomeDirectory
import com.xingpeds.kross.entities.json
import kotlinx.serialization.json.encodeToStream
import java.io.File

fun getHistoryFile(): File {
    // Get the path to the history file
    val home = getKrossHomeDirectory()
    // create a data dir and a history file under the home file

    val dataDir = File(home, "data")
    val historyFile = File(dataDir, "history.json")

    // Ensure the parent directories and the file exist
    if (!historyFile.exists()) {
        dataDir.mkdirs() // Create the data directory if it does not exist
        historyFile.createNewFile()    // Create the file if it does not exist
        json.encodeToStream(emptyList<String>(), historyFile.outputStream())
    }

    return historyFile
}