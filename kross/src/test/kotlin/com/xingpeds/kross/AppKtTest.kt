package com.xingpeds.kross

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class AppKtTest {
    @Test
    fun gitbranch() = runBlocking {
        val state = MutableStateFlow<String?>(null)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            gitBranch(state)
        }.join()
        println(state.value)
    }
}