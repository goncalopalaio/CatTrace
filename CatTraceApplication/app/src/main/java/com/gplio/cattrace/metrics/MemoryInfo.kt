package com.gplio.cattrace.metrics

import android.util.Log
import java.io.File

private const val TAG = "MemoryInfo"

data class MemoryInfo(
    val memTotalBytes: Long?,
    val memFreeBytes: Long?,
    val memAvailableBytes: Long?,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?
)

fun memoryInfo(): MemoryInfo {
    val memInfo = File("/proc/meminfo")
    val lines = memInfo.readLines()

    val memTotalBytes = findMemoryLineBytes(lines, "MemTotal:.*")
    val memFreeBytes = findMemoryLineBytes(lines, "MemFree:.*")
    val memAvailableBytes = findMemoryLineBytes(lines, "MemAvailable:.*")
    val swapTotalBytes = findMemoryLineBytes(lines, "SwapTotal:.*")
    val swapFreeBytes = findMemoryLineBytes(lines, "SwapFree:.*")

    val memoryInfo = MemoryInfo(memTotalBytes, memFreeBytes, memAvailableBytes, swapTotalBytes, swapFreeBytes)
    Log.d(TAG, "memoryInfo=$memoryInfo")

    return memoryInfo
}

fun findMemoryLineBytes(lines: List<String>, pattern: String): Long? {
    val match = lines.firstOrNull { it.matches(Regex(pattern)) } ?: return null

    return match.split(Regex("\\s+")).getOrNull(1)?.toLong()?.times(1024)
}