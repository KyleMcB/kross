package com.xingpeds.kross.state

import com.xingpeds.kross.entities.getKrossHomeDirectory
import com.xingpeds.kross.entities.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

typealias Environment = Map<String, String>

object PersistentEnvironment {
    private val _envState = MutableStateFlow<Environment>(emptyMap())
    val envState: StateFlow<Environment>
        get() = _envState
    private val scope = CoroutineScope(Dispatchers.Default)
    private val envFile: File = File(getKrossHomeDirectory(), "data/env.json").apply {
        if (!exists()) {
            parentFile.mkdirs()
            createNewFile()
            json.encodeToStream(emptyMap<String, String>(), outputStream())
        }
    }

    fun set(key: String, value: String) {
        _envState.update {
            it + mapOf(key to value)
        }
    }

    fun unSet(key: String) {
        _envState.update {
            it.minus(key)
        }
    }

    init {
        scope.launch {
            launch {
                _envState.emit(
                    json.decodeFromStream(envFile.inputStream())
                )
            }
            launch {
                // ignore initial load emit
                _envState.drop(1).collect {
                    json.encodeToStream(it, envFile.outputStream())
                }
            }
        }
    }
}
