package com.xingpeds.kross.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

interface ShellState {
    val currentDirectory: StateFlow<File>
    suspend fun changeDirectory(directory: File)
    val environment: StateFlow<Map<String, String>>
    suspend fun setVariable(name: String, value: String)
    fun setHistoryFile(file: File)
    fun addHistory(command: String)
    val history: StateFlow<List<String>>

}

@OptIn(FlowPreview::class)
object ShellStateObject : ShellState {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _currentDirectory = MutableStateFlow(File(System.getProperty("user.dir")))
    override val currentDirectory: StateFlow<File>
        get() = _currentDirectory

    override suspend fun changeDirectory(directory: File) {
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

    override fun setHistoryFile(file: File) {
        _historyFile.value = file
    }

    override fun addHistory(command: String) {
        _history.update {
            it + command
        }
    }

    private val _historyFile = MutableStateFlow<File?>(null)

    private val _history = MutableStateFlow<UserCommandHistory>(emptyList())

    init {
        scope.launch {
            launch {
                _historyFile.filterNotNull().collect { file ->
                    _history.value = loadHistory(file)
                }
            }
            launch {
                _history.drop(1).debounce(3.seconds).collect { newHistory ->
                    _historyFile.value?.let { file ->
                        saveHistory(file, newHistory)
                    }
                }
            }
        }
    }


    override val history: StateFlow<List<String>>
        get() = _history
}