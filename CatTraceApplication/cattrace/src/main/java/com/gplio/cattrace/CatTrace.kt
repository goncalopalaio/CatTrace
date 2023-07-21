package com.gplio.cattrace

import com.gplio.cattrace.events.Event
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "CatTrace"

private enum class EventType(val value: String) {
    Begin("B"),
    End("E"),
    Complete("X"),
    Instant("i"),
    Metadata("M"),
    Counter("C"),
}

enum class InstantType(val value: String) {
    Global("g"),
    Process("p"),
    Thread("t"),
}

enum class MetadataType(val value: String) {
    ProcessName("process_name"),
    ThreadName("thread_name"),
}

object CatTrace {
    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Event> = moshi.adapter(Event::class.java)

    private val threadNames = ConcurrentHashMap<Long, String>()

    private var pid = 0L

    fun setPid(pid: Long, name: String? = null) {
        this.pid = pid

        if (name == null) return

        val event = Event(
            name = MetadataType.ProcessName.value,
            eventType = EventType.Metadata.value,
            timestamp = timeUs(),
            pid = CatTrace.pid,
            tid = 0,
            arguments = mapOf("name" to name),
        )
        log(jsonAdapter.toJson(event))
    }

    fun begin(name: String, category: String? = null) {
        val event = create(EventType.Begin, name, timeUs(), category = category)
        log(jsonAdapter.toJson(event))
    }

    fun end(name: String, category: String? = null) {
        val event = create(EventType.End, name, timeUs(), category = category)
        log(jsonAdapter.toJson(event))
    }

    fun counter(name: String, arguments: Map<String, Any>, category: String? = null) {
        val event =
            create(EventType.Counter, name, timeUs(), arguments = arguments, category = category)
        log(jsonAdapter.toJson(event))
    }

    fun instant(name: String, type: InstantType = InstantType.Thread, category: String? = null) {
        val event =
            create(EventType.Instant, name, timeUs(), category = category, eventScope = type.value)
        log(jsonAdapter.toJson(event))
    }

    fun threadMetadata() {
        val timestamp = timeUs()

        for ((threadId, name) in threadNames) {
            val event = Event(
                name = MetadataType.ThreadName.value,
                eventType = EventType.Metadata.value,
                timestamp = timestamp,
                pid = pid,
                tid = threadId,
                arguments = mapOf("name" to name),
            )
            log(jsonAdapter.toJson(event))
        }

        threadNames.clear()
    }

    private fun create(
        eventType: EventType,
        name: String,
        timestamp: Long,
        category: String? = null,
        arguments: Map<String, Any>? = null,
        eventScope: String? = null
    ): Event {
        val pid = this.pid
        val currentThread = Thread.currentThread()
        val threadId = currentThread.id

        saveThreadName(threadId, currentThread.name)

        return Event(
            name = name,
            eventType = eventType.value,
            timestamp = timestamp,
            pid = pid,
            tid = threadId,
            category = category,
            arguments = arguments,
            eventScope = eventScope,
        )
    }

    private fun saveThreadName(threadId: Long, threadName: String) {
        if (threadNames.containsKey(threadId)) return
        threadNames[threadId] =
            threadName // contains and put not exactly sync'ed but duplicate metadata events aren't really an issue.
    }

    private fun timeUs() = System.currentTimeMillis() * 1000 // microseconds

    private fun log(message: String) = println("$TAG $message")
}