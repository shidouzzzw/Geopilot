package com.ymcgps.geopilot.util

import java.util.Locale

/** Enlem/boylam metinlerini ayrıştırır ve doğrular. Android'e bağımlı değildir (birim test edilebilir). */
object CoordinateParser {

    enum class Problem {
        EMPTY_LAT, EMPTY_LON, INVALID_LAT, INVALID_LON, LAT_RANGE, LON_RANGE
    }

    sealed interface Result {
        data class Success(val latitude: Double, val longitude: Double) : Result
        data class Failure(val problem: Problem) : Result
    }

    fun parse(latText: String, lonText: String): Result {
        val latRaw = latText.trim()
        val lonRaw = lonText.trim()
        if (latRaw.isEmpty()) return Result.Failure(Problem.EMPTY_LAT)
        if (lonRaw.isEmpty()) return Result.Failure(Problem.EMPTY_LON)

        val lat = parseValue(latRaw) ?: return Result.Failure(Problem.INVALID_LAT)
        val lon = parseValue(lonRaw) ?: return Result.Failure(Problem.INVALID_LON)

        if (lat < -90.0 || lat > 90.0) return Result.Failure(Problem.LAT_RANGE)
        if (lon < -180.0 || lon > 180.0) return Result.Failure(Problem.LON_RANGE)
        return Result.Success(lat, lon)
    }

    /** Ondalık ayracı olarak nokta ya da virgül kabul eder. Sonlu olmayan değerleri reddeder. */
    fun parseValue(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite()) value else null
    }

    fun isValid(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun format(value: Double): String = String.format(Locale.US, "%.6f", value)
}
