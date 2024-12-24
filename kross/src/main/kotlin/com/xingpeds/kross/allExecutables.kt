package com.xingpeds.kross

import java.io.File

fun listExecutablesOnPath(): List<String> {
    val pathEnv = System.getenv("PATH") ?: return emptyList()

    // Figure out the path separator (":" on Unix, ";" on Windows).
    val pathSeparator = File.pathSeparator

    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    // Common Windows executable extensions
    val windowsExecutableExtensions = setOf(".exe", ".bat", ".cmd", ".com")

    // Accumulate executables from each directory in PATH.
    val executables = pathEnv.split(pathSeparator)
        .flatMap { dir ->
            val folder = File(dir)
            // Get all files (if the folder exists and is actually a directory).
            val files = folder.listFiles()?.toList().orEmpty()

            files.filter { file ->
                if (!file.isFile) return@filter false

                if (isWindows) {
                    // On Windows, check if it ends with a known executable extension
                    val lowerName = file.name.lowercase()
                    val hasExecutableExt = windowsExecutableExtensions.any { lowerName.endsWith(it) }
                    hasExecutableExt && file.canExecute()
                } else {
                    // On Unix-like, just check if it’s marked as executable
                    file.canExecute()
                }
            }
        }
        // Map to the file's simple name (without path).
        .map { it.name }
        .distinct()
        .sorted()

    return executables
}
