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
import com.gplio.cattrace.application.databinding.ActivityMainBinding
import kotlin.concurrent.thread

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
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
        }

        CatTrace.setPid(System.currentTimeMillis())

        CatTrace.instant("Starting Work")
        thread(name = "thread-1") {
            CatTrace.begin("work")
            for (i in 0 until 5) {
                CatTrace.begin("work-$i", category = "work")
                slowOperation(i, 10L)
                CatTrace.end("work-$i", category = "work")

                CatTrace.counter("work", mapOf("value" to i))
            }
            CatTrace.end("work")

            CatTrace.threadMetadata()
        }

        thread(name = "thread-2") {
            CatTrace.begin("counter")
            var i = -50
            while (i <= 50) {
                CatTrace.counter("counter", mapOf("value" to i))
                i += 1
            }
            CatTrace.end("counter")

            CatTrace.threadMetadata()
        }
    }

    private fun slowOperation(to: Int, delayMillis: Long) {
        for (i in 0 until to) {
            CatTrace.begin("slowOperation-$to", category = "counter")
            Thread.sleep(delayMillis)
            CatTrace.end("slowOperation-$to", category = "counter")
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