package com.ymcgps.geopilot.mock

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.SystemClock
import com.ymcgps.geopilot.R

/**
 * Android'in resmî test sağlayıcısı (LocationManager test provider) API'sini sarar.
 * Uygulama, Geliştirici seçeneklerinde "Sahte konum uygulaması" olarak seçilmemişse
 * sistem SecurityException fırlatır; bu durum kullanıcıya açıkça bildirilir.
 */
class MockLocationEngine(private val context: Context) {

    sealed class StartResult {
        object Ok : StartResult()
        data class Failed(val message: String) : StartResult()
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private val active = mutableListOf<String>()

    fun start(): StartResult {
        var lastError: Exception? = null
        for (provider in providers) {
            try {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (_: IllegalArgumentException) {
                    // Daha önce eklenmiş bir test sağlayıcısı yok; sorun değil.
                }
                addProvider(provider)
                locationManager.setTestProviderEnabled(provider, true)
                active.add(provider)
            } catch (e: SecurityException) {
                // Uygulama sahte konum uygulaması olarak seçilmemiş.
                stop()
                return StartResult.Failed(context.getString(R.string.error_not_mock_app))
            } catch (e: Exception) {
                // Bazı cihazlarda ağ sağlayıcısı eklenemez; en az bir sağlayıcı yeterli.
                lastError = e
            }
        }
        if (active.isEmpty()) {
            return StartResult.Failed(
                context.getString(
                    R.string.error_generic,
                    lastError?.message ?: lastError?.javaClass?.simpleName.orEmpty(),
                )
            )
        }
        return StartResult.Ok
    }

    /** Konumu gönderir. Başarılıysa null, hata varsa hata metnini döndürür. */
    fun push(latitude: Double, longitude: Double): String? {
        return try {
            for (provider in active) {
                val location = Location(provider).apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    altitude = 0.0
                    accuracy = 5f
                    speed = 0f
                    bearing = 0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    verticalAccuracyMeters = 5f
                    speedAccuracyMetersPerSecond = 0.5f
                    bearingAccuracyDegrees = 10f
                }
                locationManager.setTestProviderLocation(provider, location)
            }
            null
        } catch (e: SecurityException) {
            context.getString(R.string.error_not_mock_app)
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    fun stop() {
        for (provider in active) {
            try {
                locationManager.setTestProviderEnabled(provider, false)
            } catch (_: Exception) {
            }
            try {
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {
            }
        }
        active.clear()
    }

    @Suppress("DEPRECATION")
    private fun addProvider(name: String) {
        // ProviderProperties sabitleri derleme zamanında satır içine alınır (API 26'da da güvenlidir).
        locationManager.addTestProvider(
            name,
            false, // requiresNetwork
            false, // requiresSatellite
            false, // requiresCell
            false, // hasMonetaryCost
            true,  // supportsAltitude
            true,  // supportsSpeed
            true,  // supportsBearing
            ProviderProperties.POWER_USAGE_LOW,
            ProviderProperties.ACCURACY_FINE,
        )
    }
}
