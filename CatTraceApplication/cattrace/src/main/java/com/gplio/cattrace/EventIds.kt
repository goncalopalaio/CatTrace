package com.gplio.cattrace

import java.util.concurrent.atomic.AtomicLong

object EventIds {
    private val currentEventAsyncId = AtomicLong()

    fun nextEventId() = currentEventAsyncId.incrementAndGet()
}