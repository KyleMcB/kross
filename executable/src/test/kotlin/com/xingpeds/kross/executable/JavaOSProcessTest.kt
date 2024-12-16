package com.xingpeds.kross.executable

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class JavaOSProcessTest {
    @Test
    fun one() = runTest {
        val subject = JavaOSProcess()
        val result = subject("echo", listOf("hello")).waitFor()
        assertEquals(0, result)
    }
}