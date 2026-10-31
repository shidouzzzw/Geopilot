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
import com.ymcgps.geopilot.util.applySystemInsetsAsPadding

class SetupGuideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup_guide)
        findViewById<View>(R.id.root).applySystemInsetsAsPadding()

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
