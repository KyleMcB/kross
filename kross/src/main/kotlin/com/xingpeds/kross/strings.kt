package com.xingpeds.kross

fun String.insertAt(index: Int, string: String): String {
    if (index !in 0..length) throw IndexOutOfBoundsException("Index $index out of bounds for length $length")
    return this.substring(0, index) + string + this.substring(index)
}

fun String.dropAt(index: Int): String {
    return when (index) {
        0 -> {
            this
        }

        in 1 until length -> {
            this.substring(0, index - 1) + this.substring(index)
        }

        else -> {
            this.dropLast(1)
        }
    }
}