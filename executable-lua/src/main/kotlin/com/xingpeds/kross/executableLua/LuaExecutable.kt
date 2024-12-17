package com.xingpeds.kross.executableLua

import com.xingpeds.kross.executable.Executable
import com.xingpeds.kross.executable.ExecutableResult
import com.xingpeds.kross.executable.asOutputStream
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
        // I need to construct something that I can install in global.STDIN and set as the output stream
        // Lua programs to execute
        val program = "print('hello world!')"
        val catProgram = """
        for line in io.lines() do
            print(line)
        end
        """
        val output = StringBuilder()
        global.STDOUT = adapter(output.asOutputStream())

        return ExecutableResult(
            output = output.asOutputStream(),
        ) {
            // Execute the appropriate Lua program
            global.load(if (name == "cat") catProgram else program).call()
            0
        }
    }
}

class LuaInputStream {
    fun setInput(input: InputStream) {
        input.use {}
    }

    fun getInput(): LuaBinInput = object : LuaBinInput() {
        override fun read(): Int {
            TODO("Not yet implemented")
        }

    }

}

fun adapter2(output: OutputStream): LuaBinInput = object : LuaBinInput() {
    override fun read(): Int {
        TODO("Not yet implemented")
    }

}

fun adapter(output: OutputStream): LuaWriter = object : LuaWriter() {
    override fun print(v: String) {
        output.write(v.encodeToByteArray())
    }

    override fun write(value: Int) {
        output.write(value)
    }
}

fun adapter(input: InputStream): LuaBinInput = object : LuaBinInput() {
    override fun read(): Int = input.read()

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