package com.xingpeds.kross

import java.io.File

fun initFile(): File {
    val xdgHome = System.getenv("XDG_CONFIG_HOME") ?: "${System.getProperty("user.home")}/.config"
    val filePath = "$xdgHome/kross/krossrc.lua"
    val file = File(filePath)
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }
    return file
}