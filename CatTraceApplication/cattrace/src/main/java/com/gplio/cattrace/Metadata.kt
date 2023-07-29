package com.gplio.cattrace

import com.gplio.cattrace.data.ThreadId
import com.gplio.cattrace.events.Event
import com.gplio.cattrace.types.EventType
import com.gplio.cattrace.types.MetadataType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.util.concurrent.ConcurrentHashMap

object Metadata {
    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Event> = moshi.adapter(Event::class.java)

    private val threadNames = ConcurrentHashMap<ThreadId, String>()

    fun updateThreadName(processId: Long, threadId: Long, threadName: String) {
        val key = ThreadId(processId, threadId)
        if (threadNames.containsKey(key)) return
        threadNames[key] =
            threadName // contains and put not exactly sync'ed but duplicate metadata events aren't really an issue.
    }

    /**
     * Sends the most up to date collected thread information (names).
     */
    fun sendThreadMetadata() {
        val timestamp = timeUs()

        val threadNames = popThreadNames()
        for ((threadId, name) in threadNames) {
            val event = Event(
                name = MetadataType.ThreadName.value,
                eventType = EventType.Metadata.value,
                timestamp = timestamp,
                pid = threadId.pid,
                tid = threadId.tid,
                arguments = mapOf("name" to name),
            )
            log(jsonAdapter.toJson(event))
        }
    }

    private fun popThreadNames(): HashMap<ThreadId, String> {
        val currentThreadNames = HashMap(this.threadNames)
        this.threadNames.clear()
        return currentThreadNames
    }
}