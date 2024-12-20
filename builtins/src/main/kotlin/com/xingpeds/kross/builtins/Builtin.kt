package com.xingpeds.kross.builtins

import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import java.io.File

typealias BuiltinFun = suspend (args: List<String>) -> Int


class BuiltInExecutable(private val builtin: BuiltinFun) : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>,
        cwd: File
    ): ExecutableResult {
        return {
            this@BuiltInExecutable.builtin(args)
        }
    }

}