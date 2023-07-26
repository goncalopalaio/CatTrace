package com.gplio.cattrace.application

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.gplio.cattrace.CatTrace
import com.gplio.cattrace.EventIds
import com.gplio.cattrace.application.databinding.ActivityMainBinding
import com.gplio.cattrace.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    init {
        CatTrace.setPid(System.currentTimeMillis(), name = "Sample Application Process", mapOf("type" to "START"))
    }

    override fun onCreate(savedInstanceState: Bundle?) =
        CatTrace.trace(name = "onCreate", category = TAG) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)

            val navController = findNavController(R.id.nav_host_fragment_content_main)
            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)

            binding.fab.setOnClickListener { view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .setAction("Action", null).show()

                CatTrace.instant("END_TRACE")
            }

            CatTrace.instant("Starting Work")

            coroutines()
            coroutineTiming()
            threads()

            CatTrace.threadMetadata()
        }

    private fun slowOperation(to: Int, delayMillis: Long) =
        CatTrace.trace(
            "slowOperation",
            category = TAG,
            arguments = mapOf("to" to to, "delayMillis" to delayMillis)
        ) {
            for (i in 0 until to) {
                CatTrace.begin("slowOperation-$to", category = "counter")
                Thread.sleep(delayMillis)
                CatTrace.end("slowOperation-$to", category = "counter")
            }
        }

    private fun threads() {
        val instance = CatTrace.createInstance(1, "Threads")

        thread(name = "thread-1") {
            instance.begin("work")
            for (i in 0 until 5) {
                instance.begin("work-$i", category = "work")
                slowOperation(i, i.toLong())
                instance.end("work-$i", category = "work")

                instance.counter("work", mapOf("value" to i))
            }
            instance.end("work")

            instance.threadMetadata()
        }

        thread(name = "thread-2") {
            instance.begin("counter")
            var i = -50
            while (i <= 50) {
                instance.counter("counter", mapOf("value" to i))
                i += 1
            }
            instance.end("counter")

            instance.threadMetadata()
        }

        // This doesn't work with Perfetto, the begin and end events do not connect.
        val id = EventIds.nextEventId()
        thread(name = "Switching Threads Test 1") {
            instance.begin("Switching", id = id)
            Thread.sleep(1000)
        }
        thread(name = "Switching Threads Test 2") {
            instance.begin("Switched")
            Thread.sleep(3000)
            instance.end("Switched")

            instance.end("Switching", id = id)
            instance.threadMetadata()
        }
    }

    private fun coroutines() {
        val instance = CatTrace.createInstance(2, "Coroutines")

        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            for (i in 0 until 10) {
                delay(50)
                instance.trace("delay-block-$i") {
                    delay(200L)
                }
            }
        }
        scope.launch {
            instance.trace("launch-outer", category = "coroutines") {
                var calculation = 1
                withContext(Dispatchers.Main) {
                    instance.trace("launch-work-main", category = "coroutines") {
                        delay(1000)
                        for (i in 0 until 100) {
                            calculation *= i
                        }
                        instance.counter(
                            "calculation-main",
                            mapOf("value" to calculation),
                            category = "coroutines"
                        )
                    }
                }
                withContext(Dispatchers.IO) {
                    instance.trace("launch-work-io", category = "coroutines") {
                        delay(1000)
                        for (i in 0 until 100) {
                            calculation *= i
                        }
                        instance.counter(
                            "calculation-io",
                            mapOf("value" to calculation),
                            category = "coroutines"
                        )
                    }
                }
                withContext(Dispatchers.Default) {
                    delay(1000)
                    instance.trace("launch-work-default", category = "coroutines") {
                        for (i in 0 until 100) {
                            calculation *= i
                        }
                        instance.counter(
                            "calculation-default",
                            mapOf("value" to calculation),
                            category = "coroutines"
                        )
                    }
                }
            }
        }

        scope.launch {
            CatTrace.begin("complete-switching-threads", category = "coroutines")
            delay(2000)

            val work = mutableListOf<Deferred<Int>>()
            for (i in 0 until 10) {
                work.add(async {
                    delay(2000)
                    val startTimeMs = System.currentTimeMillis()
                    val startingThread = Thread.currentThread()
                    val startingThreadId = startingThread.id
                    val startingThreadName = startingThread.name

                    withContext(Dispatchers.Main) {
                        val endingTimeMs = System.currentTimeMillis()
                        CatTrace.complete(
                            "complete-switching-threads-$i",
                            startTimeMs,
                            endingTimeMs,
                            startingThreadId = startingThreadId,
                            startingThreadName = startingThreadName, category = "coroutines"
                        )
                    }
                    i
                })
            }
            val result = work.awaitAll()
            CatTrace.end(
                "complete-switching-threads",
                arguments = mapOf("result" to result),
                category = "coroutines"
            )
        }

        scope.launch {
            delay(3000)
            withContext(Dispatchers.Main) {
                val startTimeMs = System.currentTimeMillis()
                delay(1000)
                val endingTimeMs = System.currentTimeMillis()
                CatTrace.complete(
                    "complete-same-thread",
                    startTimeMs,
                    endingTimeMs,
                    category = "coroutines"
                )
            }
        }
    }
    private fun coroutineTiming() {
        val instance = CatTrace.createInstance(50, "CoroutineTiming")
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            instance.trace("1-second") {
                delay(1000)
            }
            instance.trace("5-second") {
                delay(5000)
            }

            instance.trace("1-millisecond") {
                delay(1)
            }

            instance.threadMetadata()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}