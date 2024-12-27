package com.xingpeds.kross

import com.xingpeds.kross.entities.*
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun CoroutineScope.gitBranch(state: MutableStateFlow<String?>) = launch {
// I want to run a git program and record the output
//git rev-parse --abbrev-ref HEAD
    val output = StringBuilder()
    val exe = JavaOSProcess()
    val pipe = Chan()
    val pipes = Pipes(
        programOutput = pipe
    )
    var result = 1
    coroutineScope {
        launch {
            result = exe.invoke(
                name = "git",
                args = listOf("rev-parse", "--abbrev-ref", "HEAD"),
                pipes = pipes,
                env = ShellStateObject.environment.value,
                cwd = ShellStateObject.currentDirectory.value
            )()
            Log.info("git returned $result")
            pipe.close()
        }
        launch {
            pipe.connectTo(output.asOutputStream())
        }
    }
    if (result == 0) {
        val value = output.toString().trim()
        Log.info("emitting $value")
        state.emit(value)
    } else {
        Log.info("git failed, emitting null")
        state.emit(null)
    }

}