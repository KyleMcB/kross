package com.xingpeds.kross.entities

import java.io.File

fun getKrossHomeDirectory(): File {
    // Check for the XDG_CONFIG_HOME environment variable
    val xdgConfigHome = System.getenv("XDG_CONFIG_HOME")

    // Determine the directory path
    val krossHomePath = if (xdgConfigHome != null && xdgConfigHome.isNotBlank()) {
        "$xdgConfigHome/kross"
    } else {
        "${System.getProperty("user.home")}/.config/kross"
    }

    // Create the directory if it doesn't exist
    val krossHomeDir = File(krossHomePath)
    if (!krossHomeDir.exists()) {
        krossHomeDir.mkdirs()
    }

    return krossHomeDir
}
