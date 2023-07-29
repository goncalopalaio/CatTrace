package com.gplio.cattrace

const val CATTRACE_TAG = "CatTrace"

internal fun log(message: String) = println("$CATTRACE_TAG $message")

internal fun timeUs() = System.currentTimeMillis() * 1000 // microseconds