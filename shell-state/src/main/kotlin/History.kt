package com.xingpeds.kross.state

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

typealias UserCommandHistory = List<String>

@OptIn(ExperimentalSerializationApi::class)
fun loadHistory(file: File): UserCommandHistory {
    require(file.isFile)
    require(file.canRead())
    if (!file.exists()) {
        return emptyList() // Return an empty UserCommandHistory if the file doesn't exist
    }

    return try {
        val fileContent = file.readText() // Read file contents as a string
//        Json.decodeFromString<UserCommandHistory>(fileContent) // Deserialize JSON into UserCommandHistory
        Json.decodeFromStream<UserCommandHistory>(file.inputStream())
    } catch (e: Exception) {
        val renamedFile = File(file.parent, "invalid_${file.name}") // Prepend "invalid_" to the file name
        println("Error: invalid json file: ${file.absolutePath}, moved to ${renamedFile.absolutePath} and starting a new one")
        file.renameTo(renamedFile) // Rename the invalid file
        emptyList() // Return an empty UserCommandHistory if the file contains invalid JSON
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun saveHistory(file: File, history: UserCommandHistory) {
    require(file.exists())
    require(file.isFile)
    require(file.canWrite())
    require(file.canRead())
    //wipe file

    Json.encodeToStream(history, file.outputStream())
}