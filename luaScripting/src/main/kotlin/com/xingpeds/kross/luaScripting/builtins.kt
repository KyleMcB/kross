package com.xingpeds.kross.luaScripting

import com.xingpeds.kross.state.ShellStateObject
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

val setenv = object : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {
        if (args.narg() < 2) {
            error("setenv requires two arguments: name and value")
        }

        val name = args.arg(1).tojstring()
        val value = args.arg(2).tojstring()

        // Access ShellState and set the environment variable
        runBlocking {
            // fixme I don't like this coupling, but I'm too lazy to fix it right now
            ShellStateObject.setVariable(name, value)
        }

        return LuaValue.NONE
    }
}