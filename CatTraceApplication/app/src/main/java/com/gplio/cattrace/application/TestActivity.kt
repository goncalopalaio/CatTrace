package com.gplio.cattrace.application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.gplio.cattrace.CatTrace
import com.gplio.cattrace.application.databinding.ActivityTestBinding
import com.gplio.cattrace.trace
import kotlin.concurrent.thread

private const val TAG = "TestActivity"

class TestActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityTestBinding

    init {
        CatTrace.setPid(
            System.currentTimeMillis(),
            name = "Test",
            mapOf("type" to "START")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()
        }

        thread(name = "rootThread") {

            val threadA = thread(name = "threadA") {
                CatTrace.begin("A")
                Thread.sleep(1000)
                CatTrace.end("A")
            }


            val threadB = thread(name = "threadB") {
                CatTrace.begin("B")
                Thread.sleep(2000)
                CatTrace.end("B")
            }


            val threadC = thread(name = "threadC") {
                CatTrace.begin("C")
                Thread.sleep(3000)
                CatTrace.end("C")
            }

            val threadD = thread(name = "threadD") {
                CatTrace.begin("D-top")
                Thread.sleep(100)
                CatTrace.end("D-top")
                CatTrace.instant("Instant-1")
                Thread.sleep(100)
                CatTrace.instant("Instant-2")
                Thread.sleep(200)
                CatTrace.instant("Instant-3")
                Thread.sleep(300)
                CatTrace.instant("Instant-4")
                Thread.sleep(400)
                CatTrace.begin("D-bottom")
                Thread.sleep(100)
                CatTrace.end("D-bottom")
            }

            CatTrace.trace("join") {
                threadA.join()
                threadB.join()
                threadC.join()
                threadD.join()
            }

            CatTrace.sendThreadMetadata()
        }
    }
}