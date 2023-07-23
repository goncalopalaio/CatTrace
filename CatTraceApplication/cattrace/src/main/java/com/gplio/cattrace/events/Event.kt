package com.gplio.cattrace.events

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Event(
    @Json(name = "name") val name: String,
    @Json(name = "ph") val eventType: String,
    @Json(name = "ts") val timestamp: Long,
    @Json(name = "pid") val pid: Long,
    @Json(name = "tid") val tid: Long,
    @Json(name = "s") val eventScope: String? = null,
    @Json(name = "args") val arguments: Map<String, Any>? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "cat") val category: String? = null,
    @Json(name = "dur") val duration: Long? = null,
)