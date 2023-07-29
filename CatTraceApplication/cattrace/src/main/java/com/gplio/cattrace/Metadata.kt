package com.gplio.cattrace

import com.gplio.cattrace.data.ThreadId
import com.gplio.cattrace.events.Event
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.util.concurrent.ConcurrentHashMap

internal object Metadata {
    private val threadNames = ConcurrentHashMap<ThreadId, String>()

    fun pushThreadName(processId: Long, threadId: Long, threadName: String) {
        val key = ThreadId(processId, threadId)
        if (threadNames.containsKey(key)) return
        threadNames[key] =
            threadName // contains and put not exactly sync'ed but duplicate metadata events aren't really an issue.
    }

    fun popThreadNames(): HashMap<ThreadId, String> {
        val currentThreadNames = HashMap(this.threadNames)
        this.threadNames.clear()
        return currentThreadNames
    }
}