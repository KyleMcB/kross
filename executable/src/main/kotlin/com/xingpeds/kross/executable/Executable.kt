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
        log("Starting JavaOSProcess.invoke with parameters: name=$name, args=$args, pipes=$pipes,  cwd=$cwd")

        log("Setting up $name with command: ${listOf(name) + args}")
        val pb = ProcessBuilder(listOf(name) + args)
        pb.directory(cwd)
        log("$name working directory set to: ${pb.directory()}")
        pb.environment().clear()
        log("$name environment cleared")
        pb.environment().putAll(env)
        if (pipes.programInput != null) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
            log("$name input redirected to PIPE")
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
            log("$name input redirected to INHERIT")
        }
        if (pipes.programOutput != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
            log("$name output redirected to PIPE")
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            log("$name output redirected to INHERIT")
        }
        if (pipes.programError != null) {
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            log("$name error redirected to PIPE")
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            log("$name error redirected to INHERIT")
        }
        val process = pb.start()
        log("Process started successfully")
        coroutineScope {
            val programInput = pipes.programInput
            launch {
                if (programInput != null) {
                    programInput.connectTo(process.outputStream, autoClose = false)
                    log("$name input successfully connected to process")
                }
            }
            launch {
                val programOutput = pipes.programOutput
                if (programOutput != null) {
                    programOutput.connectTo(process.inputStream, autoClose = false)
                    log("$name output successfully connected to process")
                }
            }
            launch {
                val programError = pipes.programError
                if (programError != null) {
                    programError.connectTo(process.errorStream, autoClose = false)
                    log("$name error successfully connected to process")
                }
            }
        }.join()
        return {
            val exitCode = process.waitFor()
            pipes.programInput?.close()
            pipes.programOutput?.close()
            pipes.programError?.close()
            process.destroy()
            log("$name exited with code: $exitCode")
            exitCode
        }
    }

}
