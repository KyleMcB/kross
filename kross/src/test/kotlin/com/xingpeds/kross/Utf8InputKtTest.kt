package com.xingpeds.kross

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class Utf8InputKtTest {
    @Test
    fun emoji() = runBlocking {
//55357=56836
        val happyFaceEmoji = flowOf(55357, 56836) //😄
        val expected = "😄"
        val expectedKey = KeyEvent.Character(expected)
        val channel = Channel<Int>()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {

                val keys = toKeyEventFlow(channel).toList()
                assert(keys.first() == expectedKey)
            }
            launch {
                happyFaceEmoji.collect(channel::send)
                channel.close()
            }
        }.join()
        Unit
    }

    @Test
    fun backspace() = runBlocking {
        val backspace = flowOf(127)
        val expected = KeyEvent.Backspace
        val channel = Channel<Int>()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            launch {
                backspace.collect(channel::send)
                channel.close()
            }
            launch {
                val keys = toKeyEventFlow(channel).toList()
                assert(keys.first() == expected)
            }
        }.join()
        Unit
    }
}