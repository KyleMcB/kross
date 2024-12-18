package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.Pipes

class LuaExecutable : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        pipes: Pipes,
        env: Map<String, String>
    ): ExecutableResult {
        TODO("Not yet implemented")
    }
}