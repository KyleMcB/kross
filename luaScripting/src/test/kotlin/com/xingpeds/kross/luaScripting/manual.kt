package com.xingpeds.kross.luaScripting

import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseIoLib
import kotlin.test.Test

class Manual {
    val mockExe: (String) -> String = { it }
    val mockRun: (String) -> Unit = {}
    val globals = LuaEngine.getLuaGlobal(mockExe, mockRun)

    @Test
    fun one() {
        val persistentTable = LuaTable()
        val global1 = KrossLuaGlobal(persistentTable, mockExe, mockRun).apply {
            load(BaseLib())
            load(PackageLib())
            load(Bit32Lib())
            load(TableLib())
            load(StringLib())
            load(CoroutineLib())
            load(JseIoLib())
            load(MathLib())
            load(OsLib())

            LoadState.install(this)
            LuaC.install(this)
        }
        val global2 = KrossLuaGlobal(persistentTable, mockExe, mockRun).apply {
            LoadState.install(this)
            LuaC.install(this)
        }

        fun luaprintln(str: String) {
            println("LUA_PRINTLN: $str")
            //kotlin.io.println()
        }

// Overwrite print function
        global2["print"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val tostring = global2["tostring"]
                val out = (1..args.narg())
                    .map { tostring.call(args.arg(it)).strvalue()!!.tojstring() }
                luaprintln(out.joinToString("\t"))
                return LuaValue.NONE
            }
        }
        val table = LuaValue.tableOf(arrayOf(LuaString.valueOf("field"), LuaValue.valueOf(2)))
        global1["table"] = table
        global2["table"] = table
        val inc = "table.field = table.field + 1"
        val script = "print(table.field)"
        val chunk = global1.load(inc)
        val chunk2 = global2.load(script)
        chunk.call()
        chunk2.call()
        val other = global2.load(script)
        other.call()
        println("kotlin")
        val luaScript = "my_global_var = 10"
        val chunky = global1.load(luaScript)
        chunky.call()

// Check if the global variable is set
        val myGlobalVar = global2["my_global_var"]
        if (myGlobalVar.isnil()) {
            println("The global variable 'my_global_var' is not set.")
        } else {
            println("The global variable 'my_global_var' is set to: ${myGlobalVar.tojstring()}")
        }
    }

    @Test
    fun hi() {
        val globals = LuaEngine.getLuaGlobal({ it }, {})

        fun luaprintln(str: String) {
            println("LUA_PRINTLN: $str")
            //kotlin.io.println()
        }

// Overwrite print function
        globals["print"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val tostring = globals["tostring"]
                val out = (1..args.narg())
                    .map { tostring.call(args.arg(it)).strvalue()!!.tojstring() }
                luaprintln(out.joinToString("\t"))
                return LuaValue.NONE
            }
        }
        val result = globals.load(
            //language=lua
            """
    function max(a, b)
        if (a > b) then
            return a
        else
            return b
        end
    end
    a = 10
    res = 1 + 2 + a + max(20, 30)
    print(res - 1)
    b = {}
    b[1] = 10
    print(b)
    for i=4,1,-1 do print(i) end
    
    
    co = coroutine.create(function ()
       for i=1,5 do
         --print("co", i)
         coroutine.yield("co" .. i, i + 1)
       end
       return "completed"
     end)
     
    function coroutine_it (co)
      return function ()
            local code, res = coroutine.resume(co)
            if code then
                return res 
            end
     end
    end

    for i in coroutine_it(co) do
        print("for", i)
    end
    
    --for i=1,12 do
    --    local code, res = coroutine.resume(co)
    --    print(code, res)
    --end
    print("ENDED!")

    return res
"""
        )

        result.call()
    }
}