# CatTrace

When everything else fails, you can always count with printf() debugging, in this case, printf() profiling.

- The client prints events to stdout.
- The script reads the events and convert them into a Trace Event (https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1)
- You open the file in a Trace Event viewer such as https://ui.perfetto.dev/


Pipe the events into the script:

```bash
adb -s b8435fb0 logcat -v time | grep --line-buffered -Ei "CatTrace" | python3 cattrace.py
```

Open the .json file in https://ui.perfetto.dev/

![](https://github.com/goncalopalaio/CatTrace/blob/main/screenshots/screen1.png?raw=true)

How to send events:

```kotlin
// Setup
CatTrace.setPid(System.currentTimeMillis()) // Or a call to myPid()
CatTrace.sync()        
```

```kotlin
// Instant event
CatTrace.instant("Start", CatTrace.InstantType.Global)
```

```kotlin
// Complete event
CatTrace.complete("Event that started and ended", startTime, endTime)
```

```kotlin
// Event
val event = CatTrace.begin("Long Operation")
// Long operation
event.end()
```

Drop-in client implementation for Android:

```kotlin
import java.util.concurrent.ConcurrentHashMap

object CatTrace {
    private const val TAG = "CatTrace"
    private val threadNames = ConcurrentHashMap<Long, String>()

    private var pid = 0L

    /**
     * Loosely follows [https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU] (Trace Event Format)
     */
    private enum class Event(val value: String) {
        Sync("clock_sync"),
        Begin("B"),
        End("E"),
        Complete("X"),
        Instant("i"),
        Metadata("M"),
    }

    enum class InstantType(val value: String) {
        Global("g"),
        Process("p"),
        Thread("t"),
    }

    enum class MetadataType(val value: String) {
        ProcessName("process_name"),
        ThreadName("thread_name"),
    }

    private class Info(val pid: Long, val threadId: Long, val threadName: String) {
        override fun toString() = "$pid|$threadId|$threadName"
    }

    class EndEvent(private val name: String) {
        fun end() = end(name)
    }

    fun setPid(pid: Long) {
        this.pid = pid
    }

    fun sync() {
        val time = time()
        val pid = this.pid
        log("${TAG}|${Event.Sync.value}|$pid|${time}")
    }

    fun begin(name: String): EndEvent {
        beginOrEnd(true, name)
        return EndEvent(name)
    }

    fun end(name: String) = beginOrEnd(false, name)

    fun complete(name: String, startTime: Long, endTime: Long) {
        val info = info()
        val startTimeUs = startTime * 1000 // microseconds
        val duration = (endTime - startTime) * 1000 // milliseconds to microseconds

        checkSendThreadName(info.pid, info.threadId, info.threadName)
        log("$TAG|${Event.Complete.value}|$info|$startTimeUs|$duration|$name")
    }

    fun instant(name: String, type: InstantType) {
        val time = time()
        val info = info()

        checkSendThreadName(info.pid, info.threadId, info.threadName)
        log("$TAG|${Event.Instant.value}|$info|${type.value}|$time|$name")
    }

    private fun metadataText(name: String, pid: Long, threadId: Long, type: MetadataType) {
        log("$TAG|${Event.Metadata.value}|$pid|$threadId|${type.value}|$name")
    }

    private fun beginOrEnd(isBegin: Boolean, name: String) {
        val time = time()
        val info = info()
        val type = if (isBegin) Event.Begin.value else Event.End.value

        checkSendThreadName(info.pid, info.threadId, info.threadName)
        log("$TAG|$type|$info|$time|$name")
    }

    private fun info(): Info {
        val pid = this.pid
        val currentThread = Thread.currentThread()
        val threadId = currentThread.id
        val threadName = currentThread.name

        return Info(pid, threadId, threadName)
    }

    private fun checkSendThreadName(pid: Long, threadId: Long, threadName: String) {
        if (threadNames.containsKey(threadId)) return
        threadNames[threadId] = threadName // contains and put not exactly sync'ed but duplicate metadata events aren't really an issue.

        metadataText(threadName, pid, threadId, MetadataType.ThreadName)
    }
    private fun time() = System.currentTimeMillis() * 1000 // microseconds

    private fun log(message: String) = println(message)
}

```


