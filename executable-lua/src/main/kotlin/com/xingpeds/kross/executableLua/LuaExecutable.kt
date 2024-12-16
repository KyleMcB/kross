package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.luaScripting.LuaEngine
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import java.io.InputStream
import java.io.OutputStream

class LuaExecutable : Executable {
    override suspend fun invoke(
        name: String,
        args: List<String>,
        input: Executable.StreamSetting,
        output: Executable.StreamSetting,
        error: Executable.StreamSetting
    ): ExecutableResult {
        val global = LuaEngine.global
        val program = "print('hello world!')"
        val catProgram = """
            for line in io.lines() do
                print(line)
            end
        """
        return ExecutableResult(output = adapter(global.STDOUT)) {
            global.load(if (name == "cat") catProgram else program).call()
            0
        }
    }
}

fun adapter(luaIn: LuaBinInput): InputStream = object : InputStream() {
    override fun read(): Int =
        luaIn.read()
}

fun adapter(luaOut: LuaWriter): OutputStream = object : OutputStream() {
    override fun write(b: Int) {
        luaOut.write(b)
    }
}