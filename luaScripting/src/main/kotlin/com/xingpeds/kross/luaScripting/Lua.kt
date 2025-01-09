package com.xingpeds.kross.luaScripting

import com.xingpeds.kross.state.Builtin
import com.xingpeds.kross.state.BuiltinFun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.io.LuaWriter
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseIoLib
import java.io.*
import kotlin.reflect.KClass

interface Lua {
    suspend fun executeLua(
        code: String,
        input: InputStream? = null,
        output: OutputStream? = null,
        error: OutputStream? = null
    )

    suspend fun userFuncExists(name: String): Boolean
    suspend fun userFuncs(): Set<String>
}

suspend fun Lua.executeFile(
    file: File,
    input: InputStream? = null,
    output: OutputStream? = null,
    error: OutputStream? = null
) {
    require(file.exists()) { "File does not exist: $file" }
    val codeAsText: String = file.readText()
    this.executeLua(codeAsText, input, output, error)
}

fun String.toLua(): LuaValue = LuaValue.valueOf(this)

fun adapter(builtin: BuiltinFun): LuaFunction = object : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs = runBlocking {
        val argList = mutableListOf<String>()
        for (i in 1..args.narg()) {
            val arg = args.arg(i)
            argList.add(arg.tojstring())
        }
        LuaValue.varargsOf(arrayOf(LuaValue.valueOf(builtin(argList))))
    }
}

fun LuaValue.key(name: String): LuaValue? = try {
    get(name)
} catch (e: Exception) {
    null
}

class UserDisplayError(override val message: String) : Exception()
data class UserLuaFunction(val name: String, val desc: String, val callback: LuaFunction)
object LuaEngine : Lua {
    val root = LuaValue.tableOf()
    val _userFunctions = MutableStateFlow<Map<String, UserLuaFunction>>(emptyMap())
    val userTable = LuaValue.tableOf()
    val builtinTable = LuaValue.tableOf().apply {
        Builtin.builtinFuns.forEach { (name: String, func: BuiltinFun) ->
            this[name] = adapter(func)
        }
    }

    /*
    map {
    name =
    desc =
    callback =
    }
     */
    val registerFunction = object : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val name = arg.key("name")?.checkjstring() ?: throw UserDisplayError("function name not supplied")
            val desc = arg.key("desc")?.checkjstring() ?: throw UserDisplayError("function desc not supplied")
            val callback =
                arg.key("callback")?.checkfunction() ?: throw UserDisplayError("function callback not supplied")
            _userFunctions.update {
                it.toMutableMap().apply { this[name] = UserLuaFunction(name, desc, callback) }
            }
            return LuaValue.NIL
        }

    }

    val apiTable = LuaValue.tableOf().apply {
        this["register"] = registerFunction
    }            // Create the `api` table
    val krossTable = LuaValue.tableOf().apply {
        this["api"] = apiTable
        this["userFuncs"] = userTable
        this["builtin"] = builtinTable
    }          // Create the `kross` table
    val global = Globals().apply {
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
        this["kross"] = krossTable                   // Add the `kross` table to Globals
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            _userFunctions.collect { userFuncMap ->

                userFuncMap.values.forEach { userLuaFunc: UserLuaFunction ->
                    val (name, desc, func) = userLuaFunc
                    val table = LuaValue.tableOf(
                        arrayOf(
                            LuaValue.valueOf("name"),
                            LuaString.valueOf(name),
                            LuaValue.valueOf("desc"),
                            LuaString.valueOf(desc),
                            LuaValue.valueOf("callback"),
                            func
                        )
                    )
                    userTable[name] = table
                    global["userFuncs"] = userTable
                }
            }
        }
    }

    override suspend fun executeLua(code: String, input: InputStream?, output: OutputStream?, error: OutputStream?) {

        val originalStdout = global.STDOUT
        val originalStdin = global.STDIN
        val originalStderr = global.STDERR

        try {
            // Override streams if provided
            if (output != null) global.STDOUT = outputAdapter(output)
            if (input != null) global.STDIN = inputAdapter(input)
            if (error != null) global.STDERR = outputAdapter(error)

            global.load(code).call()
        } finally {
            // Restore original streams
            global.STDOUT = originalStdout
            global.STDIN = originalStdin
            global.STDERR = originalStderr
        }
    }

    override suspend fun userFuncExists(name: String): Boolean {
        val containsKey = _userFunctions.value.containsKey(name)
        return containsKey
    }

    override suspend fun userFuncs(): Set<String> {
        return _userFunctions.value.keys
    }


}

fun outputAdapter(output: OutputStream): LuaWriter = object : LuaWriter() {
    private val writer = PrintWriter(output, true)

    override fun print(v: String) {
        writer.print(v)
        writer.flush()
    }

    override fun write(value: Int) {
        writer.write(value)
        writer.flush()
    }

}

fun inputAdapter(input: InputStream): LuaBinInput = object : LuaBinInput() {
    private val reader = BufferedReader(InputStreamReader(input))

    override fun read(): Int = reader.read()
}

class KrossLuaGlobal(val globalsTable: LuaTable) : Globals() {

    override fun checkglobals(): Globals {
        return this
    }

    override fun isTailcall(): Boolean = globalsTable.isTailcall()

    override fun eval(): Varargs {
        return globalsTable.eval()
    }

    override fun copyto(dest: Array<LuaValue>, offset: Int, length: Int) {
        globalsTable.copyto(dest, offset, length)
    }

    override fun wrap(value: LuaValue): LuaValue {
        return globalsTable.wrap(value)
    }

    override fun useWeakValues(): Boolean {
        return globalsTable.useWeakValues()
    }

    override fun useWeakKeys(): Boolean {
        return globalsTable.useWeakKeys()
    }

    override fun typename(): String {
        return globalsTable.typename()
    }

    override fun type(): Int {
        return globalsTable.type()
    }

    override fun toLuaValue(): LuaValue {
        return globalsTable.toLuaValue()
    }

    override fun setmetatable(metatable: LuaValue?): LuaValue {
        return globalsTable.setmetatable(metatable)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        globalsTable.set(key, value)
    }

    override fun set(key: Int, value: LuaValue) {
        globalsTable.set(key, value)
    }

    override fun rawset(key: LuaValue, value: LuaValue) {
        globalsTable.rawset(key, value)
    }

    override fun rawset(key: Int, value: LuaValue) {
        globalsTable.rawset(key, value)
    }

    override fun rawlen(): Int {
        return globalsTable.rawlen()
    }

    override fun rawget(key: LuaValue): LuaValue {
        return globalsTable.rawget(key)
    }

    override fun rawget(key: Int): LuaValue {
        return globalsTable.rawget(key)
    }

    override fun presize(narray: Int) {
        globalsTable.presize(narray)
    }

    override fun opttable(defval: LuaTable?): LuaTable? {
        return globalsTable.opttable(defval)
    }

    override fun next(key: LuaValue): Varargs {
        return globalsTable.next(key)
    }

    override fun length(): Int {
        return globalsTable.length()
    }

    override fun len(): LuaValue {
        return globalsTable.len()
    }

    override fun istable(): Boolean {
        return globalsTable.istable()
    }

    override fun inext(key: LuaValue): Varargs {
        return globalsTable.inext(key)
    }

    override fun getmetatable(): LuaValue? {
        return globalsTable.getmetatable()
    }

    override fun get(key: LuaValue): LuaValue {
        return globalsTable.get(key)
    }

    override fun get(key: Int): LuaValue {
        return globalsTable.get(key)
    }

    override fun eq_b(`val`: LuaValue): Boolean {
        return globalsTable.eq_b(`val`)
    }

    override fun eq(`val`: LuaValue): LuaValue {
        return globalsTable.eq(`val`)
    }

    override fun entry(key: LuaValue, value: LuaValue): Slot? {
        return globalsTable.entry(key, value)
    }

    override fun checktable(): LuaTable? {
        return globalsTable.checktable()
    }

    override fun arrayget(array: Array<LuaValue?>, index: Int): LuaValue? {
        return globalsTable.arrayget(array, index)
    }

    override fun touserdata(c: KClass<*>): Any? {
        return globalsTable.touserdata(c)
    }

    override fun touserdata(): Any? {
        return globalsTable.touserdata()
    }

    override fun tostring(): LuaValue {
        return globalsTable.tostring()
    }

    override fun toshort(): Short {
        return globalsTable.toshort()
    }

    override fun tonumber(): LuaValue {
        return globalsTable.tonumber()
    }

    override fun tolong(): Long {
        return globalsTable.tolong()
    }

    override fun tojstring(): String {
        return globalsTable.tojstring()
    }

    override fun toint(): Int {
        return globalsTable.toint()
    }

    override fun tofloat(): Float {
        return globalsTable.tofloat()
    }

    override fun todouble(): Double {
        return globalsTable.todouble()
    }

    override fun tochar(): Char {
        return globalsTable.tochar()
    }

    override fun tobyte(): Byte {
        return globalsTable.tobyte()
    }

    override fun toboolean(): Boolean {
        return globalsTable.toboolean()
    }

    override fun toString(): String {
        return globalsTable.toString()
    }

    override fun subargs(start: Int): Varargs {
        return globalsTable.subargs(start)
    }

    override fun subFrom(lhs: Int): LuaValue {
        return globalsTable.subFrom(lhs)
    }

    override fun subFrom(lhs: Double): LuaValue {
        return globalsTable.subFrom(lhs)
    }

    override fun sub(rhs: LuaValue): LuaValue {
        return globalsTable.sub(rhs)
    }

    override fun sub(rhs: Int): LuaValue {
        return globalsTable.sub(rhs)
    }

    override fun sub(rhs: Double): LuaValue {
        return globalsTable.sub(rhs)
    }

    override fun strvalue(): LuaString? {
        return globalsTable.strvalue()
    }

    override fun strongvalue(): LuaValue? {
        return globalsTable.strongvalue()
    }

    override fun strcmp(rhs: LuaValue): Int {
        return globalsTable.strcmp(rhs)
    }

    override fun strcmp(rhs: LuaString): Int {
        return globalsTable.strcmp(rhs)
    }

    override fun raweq(`val`: LuaValue): Boolean {
        return globalsTable.raweq(`val`)
    }

    override fun raweq(`val`: LuaUserdata): Boolean {
        return globalsTable.raweq(`val`)
    }

    override fun raweq(`val`: LuaString): Boolean {
        return globalsTable.raweq(`val`)
    }

    override fun raweq(`val`: Int): Boolean {
        return globalsTable.raweq(`val`)
    }

    override fun raweq(`val`: Double): Boolean {
        return globalsTable.raweq(`val`)
    }

    override fun powWith(lhs: Int): LuaValue {
        return globalsTable.powWith(lhs)
    }

    override fun powWith(lhs: Double): LuaValue {
        return globalsTable.powWith(lhs)
    }

    override fun pow(rhs: LuaValue): LuaValue {
        return globalsTable.pow(rhs)
    }

    override fun pow(rhs: Int): LuaValue {
        return globalsTable.pow(rhs)
    }

    override fun pow(rhs: Double): LuaValue {
        return globalsTable.pow(rhs)
    }

    override fun optvalue(defval: LuaValue): LuaValue {
        return globalsTable.optvalue(defval)
    }

    override fun optuserdata(c: KClass<*>, defval: Any?): Any? {
        return globalsTable.optuserdata(c, defval)
    }

    override fun optuserdata(defval: Any?): Any? {
        return globalsTable.optuserdata(defval)
    }

    override fun optthread(defval: LuaThread?): LuaThread? {
        return globalsTable.optthread(defval)
    }

    override fun optstring(defval: LuaString?): LuaString? {
        return globalsTable.optstring(defval)
    }

    override fun optnumber(defval: LuaNumber?): LuaNumber? {
        return globalsTable.optnumber(defval)
    }

    override fun optlong(defval: Long): Long {
        return globalsTable.optlong(defval)
    }

    override fun optjstring(defval: String?): String? {
        return globalsTable.optjstring(defval)
    }

    override fun optinteger(defval: LuaInteger?): LuaInteger? {
        return globalsTable.optinteger(defval)
    }

    override fun optint(defval: Int): Int {
        return globalsTable.optint(defval)
    }

    override fun optfunction(defval: LuaFunction?): LuaFunction? {
        return globalsTable.optfunction(defval)
    }

    override fun optdouble(defval: Double): Double {
        return globalsTable.optdouble(defval)
    }

    override fun optclosure(defval: LuaClosure?): LuaClosure? {
        return globalsTable.optclosure(defval)
    }

    override fun optboolean(defval: Boolean): Boolean {
        return globalsTable.optboolean(defval)
    }

    override fun onInvoke(args: Varargs): Varargs {
        return globalsTable.onInvoke(args)
    }

    override fun not(): LuaValue {
        return globalsTable.not()
    }

    override fun neg(): LuaValue {
        return globalsTable.neg()
    }

    override fun narg(): Int {
        return globalsTable.narg()
    }

    override fun mul(rhs: LuaValue): LuaValue {
        return globalsTable.mul(rhs)
    }

    override fun mul(rhs: Int): LuaValue {
        return globalsTable.mul(rhs)
    }

    override fun mul(rhs: Double): LuaValue {
        return globalsTable.mul(rhs)
    }

    override fun modFrom(lhs: Double): LuaValue {
        return globalsTable.modFrom(lhs)
    }

    override fun mod(rhs: LuaValue): LuaValue {
        return globalsTable.mod(rhs)
    }

    override fun mod(rhs: Int): LuaValue {
        return globalsTable.mod(rhs)
    }

    override fun mod(rhs: Double): LuaValue {
        return globalsTable.mod(rhs)
    }

    override fun lteq_b(rhs: LuaValue): Boolean {
        return globalsTable.lteq_b(rhs)
    }

    override fun lteq_b(rhs: Int): Boolean {
        return globalsTable.lteq_b(rhs)
    }

    override fun lteq_b(rhs: Double): Boolean {
        return globalsTable.lteq_b(rhs)
    }

    override fun lteq(rhs: LuaValue): LuaValue {
        return globalsTable.lteq(rhs)
    }

    override fun lteq(rhs: Int): LuaValue {
        return globalsTable.lteq(rhs)
    }

    override fun lteq(rhs: Double): LuaValue {
        return globalsTable.lteq(rhs)
    }

    override fun lt_b(rhs: LuaValue): Boolean {
        return globalsTable.lt_b(rhs)
    }

    override fun lt_b(rhs: Int): Boolean {
        return globalsTable.lt_b(rhs)
    }

    override fun lt_b(rhs: Double): Boolean {
        return globalsTable.lt_b(rhs)
    }

    override fun lt(rhs: LuaValue): LuaValue {
        return globalsTable.lt(rhs)
    }

    override fun lt(rhs: Int): LuaValue {
        return globalsTable.lt(rhs)
    }

    override fun lt(rhs: Double): LuaValue {
        return globalsTable.lt(rhs)
    }

    override fun isvalidkey(): Boolean {
        return globalsTable.isvalidkey()
    }

    override fun isuserdata(c: KClass<*>): Boolean {
        return globalsTable.isuserdata(c)
    }

    override fun isuserdata(): Boolean {
        return globalsTable.isuserdata()
    }

    override fun isthread(): Boolean {
        return globalsTable.isthread()
    }

    override fun isstring(): Boolean {
        return globalsTable.isstring()
    }

    override fun isnumber(): Boolean {
        return globalsTable.isnumber()
    }

    override fun isnil(): Boolean {
        return globalsTable.isnil()
    }

    override fun islong(): Boolean {
        return globalsTable.islong()
    }

    override fun isinttype(): Boolean {
        return globalsTable.isinttype()
    }

    override fun isint(): Boolean {
        return globalsTable.isint()
    }

    override fun isfunction(): Boolean {
        return globalsTable.isfunction()
    }

    override fun isclosure(): Boolean {
        return globalsTable.isclosure()
    }

    override fun isboolean(): Boolean {
        return globalsTable.isboolean()
    }

    override fun invoke(args: Varargs): Varargs {
        return globalsTable.invoke(args)
    }

    override fun gteq_b(rhs: LuaValue): Boolean {
        return globalsTable.gteq_b(rhs)
    }

    override fun gteq_b(rhs: Int): Boolean {
        return globalsTable.gteq_b(rhs)
    }

    override fun gteq_b(rhs: Double): Boolean {
        return globalsTable.gteq_b(rhs)
    }

    override fun gteq(rhs: LuaValue): LuaValue {
        return globalsTable.gteq(rhs)
    }

    override fun gteq(rhs: Int): LuaValue {
        return globalsTable.gteq(rhs)
    }

    override fun gteq(rhs: Double): LuaValue {
        return globalsTable.gteq(rhs)
    }

    override fun gt_b(rhs: LuaValue): Boolean {
        return globalsTable.gt_b(rhs)
    }

    override fun gt_b(rhs: Int): Boolean {
        return globalsTable.gt_b(rhs)
    }

    override fun gt_b(rhs: Double): Boolean {
        return globalsTable.gt_b(rhs)
    }

    override fun gt(rhs: LuaValue): LuaValue {
        return globalsTable.gt(rhs)
    }

    override fun gt(rhs: Int): LuaValue {
        return globalsTable.gt(rhs)
    }

    override fun gt(rhs: Double): LuaValue {
        return globalsTable.gt(rhs)
    }

    override fun equals(obj: Any?): Boolean {
        return globalsTable.equals(obj)
    }

    override fun divInto(lhs: Double): LuaValue {
        return globalsTable.divInto(lhs)
    }

    override fun div(rhs: LuaValue): LuaValue {
        return globalsTable.div(rhs)
    }

    override fun div(rhs: Int): LuaValue {
        return globalsTable.div(rhs)
    }

    override fun div(rhs: Double): LuaValue {
        return globalsTable.div(rhs)
    }

    override fun concatTo(lhs: LuaString): LuaValue {
        return globalsTable.concatTo(lhs)
    }

    override fun concatTo(lhs: LuaNumber): LuaValue {
        return globalsTable.concatTo(lhs)
    }

    override fun concat(rhs: LuaValue): LuaValue {
        return globalsTable.concat(rhs)
    }

    override fun concat(rhs: Buffer): Buffer {
        return globalsTable.concat(rhs)
    }

    override fun checkuserdata(c: KClass<*>): Any? {
        return globalsTable.checkuserdata(c)
    }

    override fun checkuserdata(): Any? {
        return globalsTable.checkuserdata()
    }

    override fun checkthread(): LuaThread? {
        return globalsTable.checkthread()
    }

    override fun checkstring(): LuaString {
        return globalsTable.checkstring()
    }

    override fun checknumber(msg: String): LuaNumber {
        return globalsTable.checknumber(msg)
    }

    override fun checknumber(): LuaNumber? {
        return globalsTable.checknumber()
    }

    override fun checknotnil(): LuaValue {
        return globalsTable.checknotnil()
    }

    override fun checklong(): Long {
        return globalsTable.checklong()
    }

    override fun checkjstring(): String? {
        return globalsTable.checkjstring()
    }

    override fun checkinteger(): LuaInteger? {
        return globalsTable.checkinteger()
    }

    override fun checkint(): Int {
        return globalsTable.checkint()
    }

    override fun checkfunction(): LuaFunction? {
        return globalsTable.checkfunction()
    }

    override fun checkdouble(): Double {
        return globalsTable.checkdouble()
    }

    override fun checkclosure(): LuaClosure? {
        return globalsTable.checkclosure()
    }

    override fun checkboolean(): Boolean {
        return globalsTable.checkboolean()
    }

    override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
        return globalsTable.call(arg1, arg2, arg3)
    }

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        return globalsTable.call(arg1, arg2)
    }

    override fun call(arg: LuaValue): LuaValue {
        return globalsTable.call(arg)
    }

    override fun call(): LuaValue {
        return globalsTable.call()
    }

    override fun arg1(): LuaValue {
        return globalsTable.arg1()
    }

    override fun arg(index: Int): LuaValue {
        return globalsTable.arg(index)
    }

    override fun add(rhs: LuaValue): LuaValue {
        return globalsTable.add(rhs)
    }

    override fun add(rhs: Int): LuaValue {
        return globalsTable.add(rhs)
    }

    override fun add(rhs: Double): LuaValue {
        return globalsTable.add(rhs)
    }

}