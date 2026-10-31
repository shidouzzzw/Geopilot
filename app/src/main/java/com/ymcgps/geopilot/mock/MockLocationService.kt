package com.ymcgps.geopilot.mock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ymcgps.geopilot.MainActivity
import com.ymcgps.geopilot.R
import com.ymcgps.geopilot.util.CoordinateParser

/**
 * Simülasyonu ön plan hizmeti olarak çalıştırır. Çalışırken kalıcı ve görünür bir bildirim gösterir
 * (kullanıcıdan gizli çalışmaz). Konumu her saniye yeniler.
 */
class MockLocationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var engine: MockLocationEngine? = null
    private var latitude = 0.0
    private var longitude = 0.0

    private val ticker = object : Runnable {
        override fun run() {
            if (!pushLocation()) return
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            else -> shutdown(null)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine = null
        if (SimulationState.running) {
            SimulationState.update(false, latitude, longitude)
        }
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val lat = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
        val lon = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)

        if (!CoordinateParser.isValid(lat, lon)) {
            enterForeground()
            shutdown(getString(R.string.error_invalid_coords))
            return
        }
        latitude = lat
        longitude = lon

        // Hizmet zaten çalışıyorsa yalnızca hedef konumu güncelle.
        if (engine != null) {
            if (pushLocation()) {
                notificationManager().notify(NOTIFICATION_ID, buildNotification())
                SimulationState.update(true, latitude, longitude)
            }
            return
        }

        if (!enterForeground()) {
            shutdown(getString(R.string.error_generic, getString(R.string.error_foreground)))
            return
        }

        val newEngine = MockLocationEngine(this)
        when (val result = newEngine.start()) {
            MockLocationEngine.StartResult.Ok -> {
                engine = newEngine
                if (pushLocation()) {
                    SimulationState.update(true, latitude, longitude)
                    handler.postDelayed(ticker, UPDATE_INTERVAL_MS)
                }
            }
            is MockLocationEngine.StartResult.Failed -> shutdown(result.message)
        }
    }

    private fun pushLocation(): Boolean {
        val current = engine ?: return false
        val error = current.push(latitude, longitude)
        if (error != null) {
            shutdown(getString(R.string.error_push, error))
            return false
        }
        return true
    }

    private fun shutdown(error: String?) {
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine = null
        SimulationState.update(false, latitude, longitude, error)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun enterForeground(): Boolean {
        return try {
            createChannel()
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                type,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(
                getString(
                    R.string.notif_text,
                    CoordinateParser.format(latitude),
                    CoordinateParser.format(longitude),
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_stop), stopIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.ymcgps.geopilot.action.START"
        const val ACTION_STOP = "com.ymcgps.geopilot.action.STOP"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"

        private const val CHANNEL_ID = "geopilot_simulation"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 1000L
    }
}
