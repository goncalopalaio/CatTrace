# CatTrace

When everything else fails, you can always count with printf() debugging, in this case, printf() profiling.

- The client prints events to stdout.
- The script reads the events and convert them into a Trace Event (https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1)
- You open the file in a Trace Event viewer such as https://ui.perfetto.dev/


Pipe the events into the script:

```bash
adb -s b8435fb0 logcat -v time | grep --line-buffered -Ei "CatTrace" | python3 cattrace.py
```

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

