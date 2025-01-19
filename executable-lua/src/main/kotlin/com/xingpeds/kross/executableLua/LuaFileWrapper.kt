package com.xingpeds.kross.executableLua

import com.xingpeds.kross.luaScripting.toLua
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.nio.file.Path

data class LuaFileWrapper(val file: File) : LuaTable() {


    val equals = object : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return if (arg is LuaFileWrapper) {
                (file == arg.file).toLua()
            } else LuaValue.BFALSE
        }

    }

    init {
        this["equals"] = equals
    }

    val compareTo = object : OneArgFunction() {
        override fun call(arg: LuaValue): LuaNumber {
            return if (arg is LuaFileWrapper) {
                LuaValue.valueOf(file.compareTo(arg.file))
            } else LuaValue.valueOf(-1)
        }
    }

    init {
        this["compareTo"] = compareTo
    }

    val getName = object : ZeroArgFunction() {
        override fun call(): LuaString {
            return LuaValue.valueOf(file.name)
        }
    }

    init {
        this["name"] = getName
    }

    val parent = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaFileWrapper(file.parentFile)
        }

        override fun call(arg: LuaValue): LuaValue {
            return LuaFileWrapper(file.parentFile)
        }
    }

    init {
        this["parent"] = parent
    }

    val path = object : ZeroArgFunction() {
        override fun call(): LuaString {
            return LuaValue.valueOf(file.path)
        }
    }

    init {
        this["path"] = path
    }

    val isAbsolute = object : ZeroArgFunction() {
        override fun call(): LuaBoolean {
            return file.isAbsolute.toLua()
        }
    }

    init {
        this["isAbsolute"] = isAbsolute
    }

    val absolutePath = object : ZeroArgFunction() {
        override fun call(): LuaString {
            return file.absolutePath.toLua()
        }
    }

    init {
        this["absolutePath"] = absolutePath
    }

    val absoluteFile = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return file.absoluteFile.toLua()
        }
    }

    init {
        this["absoluteFile"] = absoluteFile
    }


    val canonicalPath = object : ZeroArgFunction() {
        override fun call(): LuaString {
            return file.canonicalPath.toLua()
        }
    }

    init {
        this["canonicalPath"] = canonicalPath
    }


//    override fun toURL(): URL {
//        return super.toURL()
//    }


//    override fun toURI(): URI {
//        return super.toURI()
//    }


    //    override fun canRead(): Boolean {
//        return super.canRead()
//    }
    val canRead = object : ZeroArgFunction() {
        override fun call(): LuaBoolean {
            return file.canRead().toLua()
        }
    }

    init {
        this["canRead"] = canRead
    }


//    override fun canWrite(): Boolean {
//        return super.canWrite()
//    }

    val canWrite = object : ZeroArgFunction() {
        override fun call(): LuaBoolean {
            return file.canWrite().toLua()
        }
    }

    init {
        this["canWrite"] = canWrite
    }


    val exists = object : ZeroArgFunction() {
        override fun call(): LuaBoolean {
            return file.exists().toLua()
        }
    }

    init {
        this["exists"] = exists
    }

    val isDirectory = object : ZeroArgFunction() {
        override fun call(): LuaValue {
            return file.isDirectory.toLua()
        }

    }

    init {
        this["isDirectory"] = isDirectory
    }

    val isFile = object : ZeroArgFunction() {
        override fun call(): LuaBoolean {
            return file.isFile.toLua()
        }
    }

    init {
        this["isFile"] = isFile
    }

    override fun isFile(): Boolean {
        return super.isFile()
    }


    override fun isHidden(): Boolean {
        return super.isHidden()
    }


    override fun lastModified(): Long {
        return super.lastModified()
    }


    override fun length(): Long {
        return super.length()
    }


    override fun createNewFile(): Boolean {
        return super.createNewFile()
    }


    override fun delete(): Boolean {
        return super.delete()
    }


    override fun deleteOnExit() {
        super.deleteOnExit()
    }


    override fun list(): Array<String>? {
        return super.list()
    }


    override fun list(filter: FilenameFilter?): Array<String>? {
        return super.list(filter)
    }


    override fun listFiles(): Array<File>? {
        return super.listFiles()
    }


    override fun listFiles(filter: FilenameFilter?): Array<File>? {
        return super.listFiles(filter)
    }


    override fun listFiles(filter: FileFilter?): Array<File>? {
        return super.listFiles(filter)
    }


    override fun mkdir(): Boolean {
        return super.mkdir()
    }


    override fun mkdirs(): Boolean {
        return super.mkdirs()
    }


    override fun renameTo(dest: File?): Boolean {
        return super.renameTo(dest)
    }


    override fun setLastModified(time: Long): Boolean {
        return super.setLastModified(time)
    }


    override fun setReadOnly(): Boolean {
        return super.setReadOnly()
    }


    override fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean {
        return super.setWritable(writable, ownerOnly)
    }


    override fun setWritable(writable: Boolean): Boolean {
        return super.setWritable(writable)
    }


    override fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean {
        return super.setReadable(readable, ownerOnly)
    }


    override fun setReadable(readable: Boolean): Boolean {
        return super.setReadable(readable)
    }


    override fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean {
        return super.setExecutable(executable, ownerOnly)
    }


    override fun setExecutable(executable: Boolean): Boolean {
        return super.setExecutable(executable)
    }


    override fun canExecute(): Boolean {
        return super.canExecute()
    }


    override fun getTotalSpace(): Long {
        return super.getTotalSpace()
    }


    override fun getFreeSpace(): Long {
        return super.getFreeSpace()
    }


    override fun getUsableSpace(): Long {
        return super.getUsableSpace()
    }


    override fun toPath(): Path {
        return super.toPath()
    }
}

fun File.toLua(): LuaValue {
    return LuaFileWrapper(this)
}

private fun Boolean.toLua(): LuaBoolean {
    return if (this) {
        LuaValue.BTRUE
    } else LuaValue.BFALSE
}
