package com.xingpeds.kross

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class Utf8InputKtTest {
    @Test
    fun emoji() = runBlocking {
//55357=56836
        val happyFaceEmoji = flowOf(55357, 56836) //😄
        toKeyEventFlow(happyFaceEmoji).toList().also(::println)
        Unit
    }

    @Test
    fun backspace() = runBlocking {
        val backspace = flowOf(127)

        toKeyEventFlow(backspace).toList().also(::println)
        Unit
    }
}