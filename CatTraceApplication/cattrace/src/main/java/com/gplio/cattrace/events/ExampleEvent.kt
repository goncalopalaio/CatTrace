package com.gplio.cattrace.events

@JsonClass(generateAdapter = true)
data class ExampleEvent(
  val name: String,
  val categories: List<String>
)