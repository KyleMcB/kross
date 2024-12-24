import com.xingpeds.kross.listExecutablesOnPath
import kotlin.test.Test

class ShellCompleterTest {
    @Test
    fun manual() {

        /**
         * Returns a list of all executable names found in the directories specified by the PATH environment variable.
         *
         * On Unix-like systems, checks File.canExecute().
         * On Windows, checks for extensions like .exe, .bat, .cmd, etc., and also File.canExecute() as a fallback.
         */

        // Example usage:
        val execs = listExecutablesOnPath()
        println("Found ${execs.size} executables on PATH.")
        println(execs.joinToString(separator = "\n"))

    }
}


fun listUserLuaFunctions(): List<String> {
    // Return a list of global Lua function names you consider “commands”.
    return listOf("myLuaCmd", "myLuaTest", "initPlugin")
}
