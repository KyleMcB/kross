package com.xingpeds.kross.parser

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.Pipes
import com.xingpeds.kross.state.ShellState
import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.flow.StateFlow
import java.io.File


typealias BuiltinCommand = suspend (args: List<String>) -> Int

class Executor(
    private val cwd: StateFlow<File>,
    private val makeExecutable: suspend (name: String) -> Executable,
    private val pipes: Pipes = Pipes(),
    private val shellState: ShellState = ShellStateObject
) {

}
