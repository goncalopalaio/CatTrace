package com.gplio.cattrace.definition

import com.gplio.cattrace.Metadata
import com.gplio.cattrace.events.Event
import com.gplio.cattrace.log
import com.gplio.cattrace.timeUs
import com.gplio.cattrace.types.EventType
import com.gplio.cattrace.types.FlowType
import com.gplio.cattrace.types.InstantType
import com.gplio.cattrace.types.MetadataType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

private const val ARGUMENT_STARTING_THREAD_ID = "startingThreadId"
private const val ARGUMENT_STARTING_THREAD_NAME = "startingThreadName"
private const val ARGUMENT_ENDING_THREAD_ID = "endingThreadId"
private const val ARGUMENT_ENDING_THREAD_NAME = "endingThreadName"

/**
 * Prints Trace Events to stdout.
 *
 * Events are printed in JSON as defined here:
 * Trace Event Format - [https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/]
 *
 * - Async events are not supported since they aren't visible in Perfetto (would work with chrome://tracing though) [https://github.com/google/perfetto/issues/60]
 */
internal class CatTraceImpl : CatTrace {
    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Event> = moshi.adapter(Event::class.java)

    private var pid = 0L

    override fun setPid(pid: Long, name: String?, arguments: Map<String, Any>?) {
        this.pid = pid

        if (name == null) return

        val modifiedArguments = arguments?.toMutableMap() ?: LinkedHashMap()
        modifiedArguments.putAll(mapOf("name" to name))

        val currentThread = Thread.currentThread()
        val threadId = currentThread.id
        val threadName = currentThread.name

        Metadata.pushThreadName(pid, threadId, threadName)

        val event = createEvent(
            name = MetadataType.ProcessName.value,
            eventType = EventType.Metadata.value,
            timestamp = timeUs(),
            pid = pid,
            tid = threadId,
            arguments = modifiedArguments,
        )
        log(jsonAdapter.toJson(event))
    }

    override fun begin(
        name: String,
        id: Long?,
        category: String?,
        arguments: Map<String, Any>?
    ) {
        val event =
            create(
                EventType.Begin.value,
                name,
                timeUs(),
                category = category,
                arguments = arguments,
                id = id,
            )
        log(jsonAdapter.toJson(event))
    }

    override fun end(
        name: String,
        id: Long?,
        category: String?,
        arguments: Map<String, Any>?,
    ) {
        val event =
            create(
                EventType.End.value,
                name,
                timeUs(),
                category = category,
                arguments = arguments,
                id = id,
            )
        log(jsonAdapter.toJson(event))
    }

    override fun complete(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        category: String?,
        arguments: Map<String, Any>?,
        id: Long?,
        startingThreadId: Long?,
        startingThreadName: String?,
    ) {
        val duration = (endTimeMs - startTimeMs) * 1000 // microseconds
        val startTimeUs = startTimeMs * 1000 // microseconds

        val pid = this.pid
        val currentThread = Thread.currentThread()
        val threadId = currentThread.id
        val threadName = currentThread.name

        Metadata.pushThreadName(pid, threadId, threadName)
        if (startingThreadId != null && startingThreadName != null) Metadata.pushThreadName(
            pid,
            startingThreadId,
            startingThreadName
        )

        val modifiedArguments: MutableMap<String, Any> =
            arguments?.toMutableMap() ?: LinkedHashMap()
        modifiedArguments[ARGUMENT_STARTING_THREAD_ID] = startingThreadId ?: ""
        modifiedArguments[ARGUMENT_STARTING_THREAD_NAME] = startingThreadName ?: ""
        modifiedArguments[ARGUMENT_ENDING_THREAD_ID] = threadId
        modifiedArguments[ARGUMENT_ENDING_THREAD_NAME] = threadName ?: ""

        val event = createEvent(
            name = name,
            eventType = EventType.Complete.value,
            timestamp = startTimeUs,
            pid = pid,
            tid = threadId,
            category = category,
            arguments = modifiedArguments,
            duration = duration,
        )

        log(jsonAdapter.toJson(event))
    }

    override fun counter(name: String, arguments: Map<String, Any>, category: String?) {
        val event =
            create(
                EventType.Counter.value,
                name,
                timeUs(),
                arguments = arguments,
                category = category
            )
        log(jsonAdapter.toJson(event))
    }

    override fun instant(name: String, type: InstantType, category: String?) {
        val event =
            create(
                EventType.Instant.value,
                name,
                timeUs(),
                category = category,
                eventScope = type.value
            )
        log(jsonAdapter.toJson(event))
    }

    override fun flow(
        id: Long,
        name: String,
        type: FlowType,
        arguments: Map<String, Any>?,
        category: String?
    ) {
        val eventType = when (type) {
            FlowType.Start -> EventType.FlowStart
            FlowType.Step -> EventType.FlowStep
            FlowType.End -> EventType.FlowEnd
        }

        val event =
            create(
                eventType.value,
                name,
                timeUs(),
                id = id,
                arguments = arguments,
                category = category,
            )
        log(jsonAdapter.toJson(event))
    }

    override fun sendThreadMetadata() {
        val timestamp = timeUs()

        val threadNames = Metadata.popThreadNames()
        for ((threadId, name) in threadNames) {
            val event = createEvent(
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

    private fun create(
        eventType: String,
        name: String,
        timestamp: Long,
        id: Long? = null,
        category: String? = null,
        arguments: Map<String, Any>? = null,
        eventScope: String? = null
    ): Event {
        val pid = this.pid
        val currentThread = Thread.currentThread()
        val threadId = currentThread.id

        Metadata.pushThreadName(pid, threadId, currentThread.name)

        return createEvent(
            id = id,
            name = name,
            eventType = eventType,
            timestamp = timestamp,
            pid = pid,
            tid = threadId,
            category = category,
            arguments = arguments,
            eventScope = eventScope,
        )
    }

    private fun createEvent(
        name: String,
        eventType: String,
        timestamp: Long,
        pid: Long,
        tid: Long,
        eventScope: String? = null,
        arguments: Map<String, Any>? = null,
        id: Long? = null,
        category: String? = null,
        duration: Long? = null,
    ) = Event(
        id = id,
        name = name,
        eventType = eventType,
        timestamp = timestamp,
        pid = pid,
        tid = tid,
        category = category,
        arguments = arguments,
        eventScope = eventScope,
        duration = duration,
    )
}