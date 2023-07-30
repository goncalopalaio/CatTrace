package com.gplio.cattrace.definition

import com.gplio.cattrace.types.FlowType
import com.gplio.cattrace.types.InstantType

interface CatTrace {

    fun setPid(pid: Long, name: String? = null, arguments: Map<String, Any>? = null)

    fun begin(
        name: String,
        id: Long? = null,
        category: String? = null,
        arguments: Map<String, Any>? = null
    )

    fun end(
        name: String,
        id: Long? = null,
        category: String? = null,
        arguments: Map<String, Any>? = null
    )

    fun complete(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        category: String? = null,
        arguments: Map<String, Any>? = null,
        id: Long? = null,
        startingThreadId: Long? = null,
        startingThreadName: String? = null,
    )

    fun counter(name: String, arguments: Map<String, Any>, category: String? = null)

    fun instant(name: String, type: InstantType = InstantType.Thread, category: String? = null,  arguments: Map<String, Any>? = null,)

    /**
     * Sends the most up to date collected thread information (names).
     */
    fun sendThreadMetadata()
}