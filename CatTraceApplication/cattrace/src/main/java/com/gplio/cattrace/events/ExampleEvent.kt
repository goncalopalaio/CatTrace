package com.gplio.cattrace.events

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExampleEvent(
  val name: String,
  val categories: List<String>
)