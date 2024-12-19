package com.xingpeds.kross.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

interface ShellState {
    val currentDirectory: StateFlow<File>
    suspend fun changeDirectory(directory: File)
    val environment: StateFlow<Map<String, String>>
    suspend fun setVariable(name: String, value: String)
}

object ShellStateObject : ShellState {
    private val _currentDirectory = MutableStateFlow(File(System.getProperty("user.dir")))
    override val currentDirectory: StateFlow<File>
        get() = _currentDirectory

    override suspend fun changeDirectory(directory: File) {
        println("cd to ${directory.absolutePath}")
        if (directory.exists() && directory.isDirectory) {
            _currentDirectory.emit(directory)
        } else {
            throw IllegalArgumentException("The specified directory does not exist or is not a valid directory: ${directory.absolutePath}")
        }
    }

    private val _environment = MutableStateFlow(System.getenv())

    override val environment: StateFlow<Map<String, String>>
        get() = _environment

    override suspend fun setVariable(name: String, value: String) {
        _environment.update { currentEnv ->
            currentEnv.toMutableMap().apply { this[name] = value }
        }
    }

}