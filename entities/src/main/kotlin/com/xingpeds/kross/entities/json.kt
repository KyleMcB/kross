package com.xingpeds.kross.entities

val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    this.isLenient = true
    this.allowComments = true
    this.allowTrailingComma = true
}