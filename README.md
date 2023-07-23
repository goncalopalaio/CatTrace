# CatTrace

When everything else fails, you can always count with printf() debugging, in this case, printf() profiling.

- The client prints events to stdout.
- The script reads the events and convert them into a Trace Event (https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1)
- The file that is created can be open in a Trace Event viewer such as https://ui.perfetto.dev/

## Creating events

```kotlin
// Setup
CatTrace.setPid(0, name = "Process Name")    
```

```kotlin
import com.gplio.cattrace.trace

// Emit event with the start and end of a particular method
fun originalMethod(argument: String) = trace(name = "originalMethod", arguments = mapOf("argument" to argument)) {
    // Original body
}
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

```kotlin
// Counter - Track values across time.
CatTrace.counter("beans", mapOf("value" to i))
```

```kotlin
// Thread names are continuously collected.
// Call threadMetadata to send a metadata event with their names.
CatTrace.threadMetadata()
```

## Reading events

Pipe the events into the script:

```bash
adb -s b8435fb0 logcat -v time | grep --line-buffered -Ei "CatTrace" | python3 cattrace.py

```
## Viewing events

Open the .json file in https://ui.perfetto.dev/

![](https://github.com/goncalopalaio/CatTrace/blob/main/screenshots/screen1.png?raw=true)
![](https://github.com/goncalopalaio/CatTrace/blob/main/screenshots/screen2.png?raw=true)


## How to include in your project

Follow https://jitpack.io/#goncalopalaio/CatTrace/ and get the latest version.

```groovy
// settings.gradle
pluginManagement {
    repositories {
        // (...)
        maven { url 'https://jitpack.io' }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // (...)
        maven { url 'https://jitpack.io' }
    }
}
(...)
```

```groovy
// build.gradle (:app)
implementation 'com.github.goncalopalaio:CatTrace:0.3'
```

## Known issues

- Transition between threads
