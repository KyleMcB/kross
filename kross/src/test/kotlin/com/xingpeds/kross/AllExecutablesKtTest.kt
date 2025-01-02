package com.xingpeds.kross

import org.junit.jupiter.api.Test

class AllExecutablesKtTest {
    @Test
    fun manual() {
        val list = listExecutablesOnPath()
        println(list)
    }
}