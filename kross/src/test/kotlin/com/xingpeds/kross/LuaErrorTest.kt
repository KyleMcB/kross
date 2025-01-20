package com.xingpeds.kross

import com.xingpeds.kross.entities.Chan
import com.xingpeds.kross.entities.Pipes
import com.xingpeds.kross.entities.asOutputStream
import com.xingpeds.kross.entities.connectTo
import com.xingpeds.kross.luaScripting.Lua
import com.xingpeds.kross.luaScripting.LuaEngine
import com.xingpeds.kross.luaScripting.executeFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.luaj.vm2.LuaError
import kotlin.test.Test

class LuaErrorTest {
    @Test
    fun luaError() = runTest {
        val lua: Lua = LuaEngine
        val initFile = initFile()

        try {
            lua.executeFile(initFile, { it }, {})
        } catch (e: LuaError) {
            println("failed to load init file: ${initFile.canonicalPath}")
            println(e.fileline)
        }
    }

    @Test
    fun manual() = runTest {
        val lua: Lua = LuaEngine
        val initFile = initFile()

        lua.executeFile(initFile, run = { processInput(it) }, execute = { input ->
            val output = java.lang.StringBuilder()
            val pipe = Chan()
            val pipes = Pipes(programOutput = pipe)
            coroutineScope {
                launch {
                    processInput(input, pipes = pipes)
                }
                launch {
                    pipe.connectTo(output.asOutputStream())
                }
            }.join()
            output.toString()
        })
    }
}