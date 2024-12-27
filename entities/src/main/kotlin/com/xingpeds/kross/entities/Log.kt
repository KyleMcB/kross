package com.xingpeds.kross.entities

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class LogLevel {
    INFO, WARN, ERROR, DEBUG
}

object Log {
    private val logFile: File = File("${System.getProperty("user.home")}/.kross/logs/kross.log").apply {
        mkdirs()
        createNewFile()
    }
    private val lock = ReentrantLock()

    init {
        // Ensure the log directory exists
        logFile.parentFile.mkdirs()
    }

    fun log(level: LogLevel, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val logMessage = "[$timestamp] [${level.name}] $message"
        lock.withLock {
            try {
                PrintWriter(FileWriter(logFile, true)).use { writer ->
                    writer.println(logMessage)
                }
            } catch (e: Exception) {
                // If logging fails, there's not much else we can do
                System.err.println("Failed to log message: ${e.message}")
            }
        }
    }

    fun log(level: LogLevel, exception: Exception) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val logMessage = buildString {
            append("[$timestamp] [${level.name}] Exception: ${exception.message}")
            append("\n")
            append(exception.stackTraceToString())
        }
        lock.withLock {
            try {
                PrintWriter(FileWriter(logFile, true)).use { writer ->
                    writer.println(logMessage)
                }
            } catch (e: Exception) {
                // If logging fails, there's not much else we can do
                System.err.println("Failed to log exception: ${e.message}")
            }
        }
    }

    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun error(message: String) = log(LogLevel.ERROR, message)
    fun debug(message: String) = log(LogLevel.DEBUG, message)

    fun error(exception: Exception) = log(LogLevel.ERROR, exception)
    fun debug(exception: Exception) = log(LogLevel.DEBUG, exception)
}

// Convenience Extensions
fun Any?.info(prompt: String) = Log.info("$prompt: ${this.toString()}")
fun Any?.warn(prompt: String) = Log.warn("$prompt: ${this.toString()}")
fun Any?.error(prompt: String) = Log.error("$prompt: ${this.toString()}")
fun Any?.debug(prompt: String) = Log.debug("$prompt: ${this.toString()}")