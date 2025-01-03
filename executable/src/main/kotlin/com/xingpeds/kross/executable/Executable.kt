package com.xingpeds.kross.executable

import com.xingpeds.kross.entities.Log
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.connectTo
import com.xingpeds.kross.entities.error
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

typealias ExecutableResult = suspend () -> Int

interface Executable {

    suspend operator fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes = Pipes(),
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult
}

class JavaOSProcess(private val overrideName: String? = null) : Executable {

    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult {

        val pb = ProcessBuilder(listOf(overrideName ?: name) + args)
        pb.directory(cwd)
        pb.environment().clear()
        pb.environment().putAll(env)
        if (pipes.programInput != null) {
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
            Log.info("$name redirecting input to process")
        } else {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programOutput != null) {
            Log.info("$name redirecting output to process")
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        if (pipes.programError != null) {
            Log.info("$name redirecting error to process")
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        }
        val process = pb.start()
        coroutineScope {
            launch {
                pipes.programInput?.let { programInput ->
                    try {
                        val out = process.outputStream
                        Log.info("program input of $name is ${out.javaClass.simpleName}")
                        programInput.connectTo(process.outputStream, name = name)
                    } catch (e: Exception) {
                        e.error("$name failed to connect to program input")
                    }
                }
            }
            launch {
                pipes.programOutput?.let { programOutput ->
                    try {
                        programOutput.connectTo(process.inputStream, name = name)
                    } catch (e: Exception) {
                        e.error("$name failed to connect to program output")
                    }
                }
            }
            launch {
                val programError = pipes.programError
                if (programError != null) {
                    try {
                        programError.connectTo(process.errorStream)
                    } catch (e: Exception) {
                        e.error("$name failed to connect to program error")
                    }
                }
            }
        }.join()
        return {
            val exitCode = process.waitFor()
            pipes.programInput?.close()
            pipes.programOutput?.close()
            pipes.programError?.close()
            process.destroy()
            exitCode
        }
    }

}
