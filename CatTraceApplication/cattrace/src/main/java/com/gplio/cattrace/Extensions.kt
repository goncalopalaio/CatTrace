package com.gplio.cattrace

inline fun <T> trace(
    name: String,
    category: String? = null,
    arguments: Map<String, Any>? = null,
    action: () -> T
): T {
    val id = EventIds.nextEventId()

    try {
        CatTrace.begin(name, id = id, category = category, arguments = arguments)
        return action()
    } finally {
        CatTrace.end(name, id = id, category = category, arguments = arguments)
    }
}
