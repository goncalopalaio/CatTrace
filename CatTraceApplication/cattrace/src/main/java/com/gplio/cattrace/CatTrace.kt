package com.gplio.cattrace

import com.gplio.cattrace.definition.CatTraceImpl
import com.gplio.cattrace.definition.CatTrace as CatTraceInterface

private const val TAG = "CatTrace"


/**
 * Prints Trace Events to stdout.
 *
 * Events are printed in JSON as defined here:
 * Trace Event Format - [https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1]
 *
 * - Async events are not supported since they aren't visible in Perfetto (would work with chrome://tracing though) [https://github.com/google/perfetto/issues/60]
 *
 * Make direct calls to access the global instance or call [createInstance] for a local instance.
 */
object CatTrace : CatTraceInterface by CatTraceImpl()