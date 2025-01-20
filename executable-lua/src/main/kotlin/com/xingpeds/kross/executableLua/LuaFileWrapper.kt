package com.xingpeds.kross.executableLua

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path

data class LuaFileWrapper(val file: File) : LuaTable() {

    init {
        // We attach each method or property as a key in the Lua table.

        // equals
        this["equals"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return if (arg is LuaFileWrapper) {
                    (file == arg.file).toLua()
                } else LuaValue.BFALSE
            }
        }

        // compareTo
        this["compareTo"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaNumber {
                return if (arg is LuaFileWrapper) {
                    LuaValue.valueOf(file.compareTo(arg.file))
                } else {
                    LuaValue.valueOf(-1)
                }
            }
        }

        // name
        this["name"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                return LuaValue.valueOf(file.name)
            }
        }

        // parent
        this["parent"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (file.parentFile != null) {
                    LuaFileWrapper(file.parentFile)
                } else {
                    LuaValue.NIL
                }
            }
        }

        // path
        this["path"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                return LuaValue.valueOf(file.path)
            }
        }

        // isAbsolute
        this["isAbsolute"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.isAbsolute.toLua()
            }
        }

        // absolutePath
        this["absolutePath"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                return LuaValue.valueOf(file.absolutePath)
            }
        }

        // absoluteFile
        this["absoluteFile"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaFileWrapper(file.absoluteFile)
            }
        }

        // canonicalPath
        this["canonicalPath"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                return LuaValue.valueOf(file.canonicalPath)
            }
        }

        // toURI
        this["toURI"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                val uri: URI = file.toURI()
                return LuaValue.valueOf(uri.toString())
            }
        }

        // toURL (note: File#toURL is deprecated in newer Java versions; using toURI().toURL())
        this["toURL"] = object : ZeroArgFunction() {
            override fun call(): LuaString {
                val url: URL = file.toURI().toURL()
                return LuaValue.valueOf(url.toString())
            }
        }

        // canRead
        this["canRead"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.canRead().toLua()
            }
        }

        // canWrite
        this["canWrite"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.canWrite().toLua()
            }
        }

        // exists
        this["exists"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.exists().toLua()
            }
        }

        // isDirectory
        this["isDirectory"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.isDirectory.toLua()
            }
        }

        // isFile
        this["isFile"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.isFile.toLua()
            }
        }

        // isHidden
        this["isHidden"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.isHidden.toLua()
            }
        }

        // lastModified
        this["lastModified"] = object : ZeroArgFunction() {
            override fun call(): LuaNumber {
                return LuaValue.valueOf(file.lastModified().toDouble())
            }
        }

        // length
        this["length"] = object : ZeroArgFunction() {
            override fun call(): LuaNumber {
                return LuaValue.valueOf(file.length().toDouble())
            }
        }

        // createNewFile
        this["createNewFile"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.createNewFile().toLua()
            }
        }

        // delete
        this["delete"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.delete().toLua()
            }
        }

        // deleteOnExit
        this["deleteOnExit"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                file.deleteOnExit()
                return LuaValue.NIL
            }
        }

        // list
        this["list"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val arr = file.list() ?: return LuaValue.NIL
                val t = LuaTable()
                for (i in arr.indices) {
                    t.set(i + 1, LuaValue.valueOf(arr[i]))
                }
                return t
            }
        }

        // listFiles
        this["listFiles"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val arr = file.listFiles() ?: return LuaValue.NIL
                val t = LuaTable()
                for (i in arr.indices) {
                    t.set(i + 1, LuaFileWrapper(arr[i]))
                }
                return t
            }
        }

        // mkdir
        this["mkdir"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.mkdir().toLua()
            }
        }

        // mkdirs
        this["mkdirs"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.mkdirs().toLua()
            }
        }

        // renameTo
        this["renameTo"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (arg is LuaFileWrapper) {
                    return file.renameTo(arg.file).toLua()
                }
                return LuaValue.BFALSE
            }
        }

        // setLastModified
        this["setLastModified"] = object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val time = arg.optlong(0)
                return file.setLastModified(time).toLua()
            }
        }

        // setReadOnly
        this["setReadOnly"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.setReadOnly().toLua()
            }
        }

        // setWritable - handle both overloads in one VarArgFunction
        this["setWritable"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                // If one argument is given, call setWritable(boolean)
                // If two are given, call setWritable(boolean, boolean)
                val writable = args.optboolean(1, false)
                return if (args.narg() == 1) {
                    file.setWritable(writable).toLua()
                } else {
                    val ownerOnly = args.optboolean(2, false)
                    file.setWritable(writable, ownerOnly).toLua()
                }
            }
        }

        // setReadable
        this["setReadable"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                val readable = args.optboolean(1, false)
                return if (args.narg() == 1) {
                    file.setReadable(readable).toLua()
                } else {
                    val ownerOnly = args.optboolean(2, false)
                    file.setReadable(readable, ownerOnly).toLua()
                }
            }
        }

        // setExecutable
        this["setExecutable"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): LuaValue {
                val executable = args.optboolean(1, false)
                return if (args.narg() == 1) {
                    file.setExecutable(executable).toLua()
                } else {
                    val ownerOnly = args.optboolean(2, false)
                    file.setExecutable(executable, ownerOnly).toLua()
                }
            }
        }

        // canExecute
        this["canExecute"] = object : ZeroArgFunction() {
            override fun call(): LuaBoolean {
                return file.canExecute().toLua()
            }
        }

        // getTotalSpace
        this["getTotalSpace"] = object : ZeroArgFunction() {
            override fun call(): LuaNumber {
                return LuaValue.valueOf(file.totalSpace.toDouble())
            }
        }

        // getFreeSpace
        this["getFreeSpace"] = object : ZeroArgFunction() {
            override fun call(): LuaNumber {
                return LuaValue.valueOf(file.freeSpace.toDouble())
            }
        }

        // getUsableSpace
        this["getUsableSpace"] = object : ZeroArgFunction() {
            override fun call(): LuaNumber {
                return LuaValue.valueOf(file.usableSpace.toDouble())
            }
        }

        // toPath
        this["toPath"] = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val path: Path = file.toPath()
                // You could wrap Path similarly, or just return its string form:
                return LuaValue.valueOf(path.toString())
            }
        }
    }
}

// Extension function to convert File directly to Lua:
fun File.toLua(): LuaValue {
    return LuaFileWrapper(this)
}

// Extension function to convert Booleans to LuaBoolean conveniently:
private fun Boolean.toLua(): LuaBoolean {
    return if (this) LuaValue.BTRUE else LuaValue.BFALSE
}
