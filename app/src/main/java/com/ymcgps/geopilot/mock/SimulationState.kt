package com.ymcgps.geopilot.mock

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Simülasyonun anlık durumu. Hizmet ve arayüz aynı süreçte çalıştığı için basit bir tekil nesne yeterlidir.
 * Tüm güncellemeler ana iş parçacığından yapılır.
 */
object SimulationState {

    interface Listener {
        fun onSimulationChanged()
    }

    @Volatile
    var running: Boolean = false
        private set

    @Volatile
    var latitude: Double = 0.0
        private set

    @Volatile
    var longitude: Double = 0.0
        private set

    private var pendingError: String? = null
    private val listeners = CopyOnWriteArraySet<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun update(running: Boolean, latitude: Double, longitude: Double, error: String? = null) {
        this.running = running
        this.latitude = latitude
        this.longitude = longitude
        if (error != null) pendingError = error
        listeners.forEach { it.onSimulationChanged() }
    }

    /** Bekleyen hata mesajını bir kez döndürür. */
    fun consumeError(): String? {
        val error = pendingError
        pendingError = null
        return error
    }
}
