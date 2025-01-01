package com.xingpeds.kross

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyEventTest {
    @Test
    fun ctrla() {
        val a = KeyEvent.Ctrl('A')
        assertEquals(1, a.toBytes().first())
    }

    @Test
    fun ctrlb() {
        val b = KeyEvent.Ctrl('B')
        assertEquals(2, b.toBytes().first())
    }

    @Test
    fun alta() {
        val a = KeyEvent.Alt("a")
        val expected = listOf(27, 97)
        assertEquals(expected, a.toBytes())
    }
}