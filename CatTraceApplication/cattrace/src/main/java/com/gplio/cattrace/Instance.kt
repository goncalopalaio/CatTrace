package com.gplio.cattrace

import com.gplio.cattrace.definition.CatTrace
import com.gplio.cattrace.definition.CatTraceImpl

/**
 * Creates a separate, local instance of [CatTrace].
 * Used to create a separate process view in trace view.
 */
fun createInstance(
    pid: Long,
    processName: String,
    arguments: Map<String, Any>? = null,
): CatTrace =
    CatTraceImpl().apply {
        setPid(pid, processName, arguments = arguments)
    }