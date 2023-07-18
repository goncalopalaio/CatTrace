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