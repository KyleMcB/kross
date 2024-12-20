package com.xingpeds.kross.executable

import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.connectTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

typealias ExecutableResult = suspend () -> Int

private fun log(any: Any?) = println("Executable: $any")
interface Executable {

    suspend operator fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes = Pipes(),
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult
}

class JavaOSProcess : Executable {

    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult {
        log("Starting JavaOSProcess.invoke with parameters: name=$name, args=$args, pipes=$pipes, env=$env, cwd=$cwd")

        log("Setting up ProcessBuilder with command: ${listOf(name) + args}")
        val pb = ProcessBuilder(listOf(name) + args)
        pb.directory(cwd)
        log("ProcessBuilder working directory set to: ${pb.directory()}")
        pb.environment().clear()
        log("ProcessBuilder environment cleared")
        pb.environment().putAll(env)
        log("ProcessBuilder environment updated with variables: $env")
        if (pipes.programInput != null) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
            log("ProcessBuilder input redirected to PIPE")
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
            log("ProcessBuilder input redirected to INHERIT")
        }
        if (pipes.programOutput != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
            log("ProcessBuilder output redirected to PIPE")
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            log("ProcessBuilder output redirected to INHERIT")
        }
        if (pipes.programError != null) {
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            log("ProcessBuilder error redirected to PIPE")
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            log("ProcessBuilder error redirected to INHERIT")
        }
        val process = pb.start()
        log("Process started successfully")
        coroutineScope {
            val programInput = pipes.programInput
            launch {
                log("Launched coroutine to handle program input")
                if (programInput != null) {
                    programInput.connectTo(process.outputStream)
                    log("Program input successfully connected to process")
                }
            }
            launch {
                val programOutput = pipes.programOutput
                log("Launched coroutine to handle program output")
                if (programOutput != null) {
                    programOutput.connectTo(process.inputStream)
                    log("Program output successfully connected to process")
                }
            }
            launch {
                val programError = pipes.programError
                log("Launched coroutine to handle program error")
                if (programError != null) {
                    programError.connectTo(process.errorStream)
                    log("Program error successfully connected to process")
                }
            }
        }
        return {
            val exitCode = process.waitFor()
            log("Process exited with code: $exitCode")
            exitCode
        }
    }

}
