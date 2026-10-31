package com.ymcgps.geopilot.data

import android.content.Context
import com.ymcgps.geopilot.util.CoordinateParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/** Favori konumları yalnızca cihazda (SharedPreferences, JSON) saklar. Bozuk veriye karşı dayanıklıdır. */
class FavoriteStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Favorite> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { fromJson(it) }
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun add(name: String, latitude: Double, longitude: Double): Favorite {
        val favorite = Favorite(UUID.randomUUID().toString(), name, latitude, longitude)
        save(getAll() + favorite)
        return favorite
    }

    fun remove(id: String) {
        save(getAll().filterNot { it.id == id })
    }

    private fun save(list: List<Favorite>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }

    private fun toJson(f: Favorite): JSONObject = JSONObject().apply {
        put("id", f.id)
        put("name", f.name)
        put("lat", f.latitude)
        put("lon", f.longitude)
    }

    private fun fromJson(o: JSONObject): Favorite? {
        val id = o.optString("id")
        val name = o.optString("name")
        val lat = o.optDouble("lat", Double.NaN)
        val lon = o.optDouble("lon", Double.NaN)
        if (id.isBlank() || !CoordinateParser.isValid(lat, lon)) return null
        return Favorite(id, name, lat, lon)
    }

    private companion object {
        const val PREFS_NAME = "geopilot_favorites"
        const val KEY_LIST = "list"
    }
}
