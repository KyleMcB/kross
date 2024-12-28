package com.xingpeds.kross

import com.xingpeds.kross.entities.Chan
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asOutputStream
import com.xingpeds.kross.entities.connectTo
import com.xingpeds.kross.executable.JavaOSProcess
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun CoroutineScope.gitBranch(state: MutableStateFlow<String?>) = launch {
// I want to run a git program and record the output
//git rev-parse --abbrev-ref HEAD
    val output = StringBuilder()
    val exe = JavaOSProcess()
    val pipe = Chan()
    // git prints an error in every non git dir. so we throw the error output into a channel and throw it away
    val errorPipe = Channel<Int>(Channel.UNLIMITED)
    val pipes = Pipes(
        programOutput = pipe,
        programError = errorPipe,
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
            pipe.close()
            errorPipe.close()
        }
        launch {
            pipe.connectTo(output.asOutputStream())
        }
    }
    if (result == 0) {
        val value = output.toString().trim()
        state.emit(value)
    } else {
        state.emit(null)
    }

}