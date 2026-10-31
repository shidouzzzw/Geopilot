package com.ymcgps.geopilot.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ymcgps.geopilot.R
import com.ymcgps.geopilot.util.applyInsetsAsPadding

class SetupGuideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup_guide)

        val scroll = findViewById<View>(R.id.setupScroll)
        scroll.applyInsetsAsPadding(top = true, horizontal = true)
        findViewById<View>(R.id.setupActions).applyInsetsAsPadding(bottom = true, horizontal = true)
        scroll.alpha = 0f
        scroll.translationY = 24f
        scroll.animate().alpha(1f).translationY(0f).setDuration(240).start()

        findViewById<View>(R.id.openDevOptionsButton).setOnClickListener { openDeveloperOptions() }
        findViewById<View>(R.id.closeButton).setOnClickListener { finish() }
    }

    private fun openDeveloperOptions() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_dev_options, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.error_dev_options, Toast.LENGTH_LONG).show()
        }
    }
}
