package com.gplio.cattrace.types

internal enum class EventType(val value: String) {
    Begin("B"),
    End("E"),
    Complete("X"),
    Instant("i"),
    Metadata("M"),
    Counter("C"),
    FlowStart("s"),
    FlowStep("t"),
    FlowEnd("f"),
}
