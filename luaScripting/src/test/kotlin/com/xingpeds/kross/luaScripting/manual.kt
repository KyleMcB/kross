package com.xingpeds.kross.luaScripting

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import kotlin.test.Test

class Manual {
    @Test
    fun hi() {
        val globals = LuaEngine.global

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