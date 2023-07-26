package com.gplio.cattrace

import com.gplio.cattrace.definition.CatTrace as CatTraceInterface

inline fun <T> CatTraceInterface.trace(
    name: String,
    category: String? = null,
    arguments: Map<String, Any>? = null,
    action: () -> T
): T {
    val id = EventIds.nextEventId()
    val currentThread = Thread.currentThread()
    val startingThreadId = currentThread.id
    val endingThreadName = currentThread.name

    val startTimeMs = System.currentTimeMillis()
    var endTimeMs = 0L

    try {
        val result = action()
        endTimeMs = System.currentTimeMillis()
        return result
    } finally {
        this.complete(
            name,
            startTimeMs,
            endTimeMs,
            category = category,
            arguments = arguments,
            id = id,
            startingThreadId = startingThreadId,
            startingThreadName = endingThreadName
        )
    }
}
