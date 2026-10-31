package com.ymcgps.geopilot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ymcgps.geopilot.data.Favorite
import com.ymcgps.geopilot.data.FavoriteStore
import com.ymcgps.geopilot.mock.MockLocationService
import com.ymcgps.geopilot.mock.SimulationState
import com.ymcgps.geopilot.ui.SetupGuideActivity
import com.ymcgps.geopilot.util.CoordinateParser
import com.ymcgps.geopilot.util.applySystemInsetsAsPadding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

class MainActivity : AppCompatActivity(), SimulationState.Listener {

    private lateinit var root: View
    private lateinit var map: MapView
    private lateinit var latLayout: TextInputLayout
    private lateinit var lonLayout: TextInputLayout
    private lateinit var latInput: TextInputEditText
    private lateinit var lonInput: TextInputEditText
    private lateinit var statusText: TextView
    private lateinit var toggleButton: MaterialButton
    private lateinit var updateButton: MaterialButton
    private lateinit var favorites: FavoriteStore

    private var marker: Marker? = null
    private var pendingStart: GeoPoint? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val point = pendingStart
            pendingStart = null
            if (hasLocationPermission()) {
                if (point != null) startSimulationService(point)
            } else {
                showMessage(getString(R.string.permission_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initOsmdroid()
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        root.applySystemInsetsAsPadding()
        map = findViewById(R.id.map)
        latLayout = findViewById(R.id.latLayout)
        lonLayout = findViewById(R.id.lonLayout)
        latInput = findViewById(R.id.latInput)
        lonInput = findViewById(R.id.lonInput)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        updateButton = findViewById(R.id.updateButton)
        favorites = FavoriteStore(this)

        setupMap()

        findViewById<View>(R.id.showButton).setOnClickListener {
            readInput()?.let { selectPoint(it, move = true) }
        }
        findViewById<View>(R.id.addFavoriteButton).setOnClickListener { addFavorite() }
        findViewById<View>(R.id.favoritesButton).setOnClickListener { showFavorites() }
        findViewById<View>(R.id.setupButton).setOnClickListener { openSetupGuide() }
        toggleButton.setOnClickListener { onToggleClicked() }
        updateButton.setOnClickListener {
            readInput()?.let { point ->
                selectPoint(point, move = false)
                // Güncelleme de başlatma ile aynı izin akışından geçmeli.
                requestStart(point)
            }
        }

        if (savedInstanceState != null) {
            val lat = savedInstanceState.getDouble(STATE_LAT, Double.NaN)
            val lon = savedInstanceState.getDouble(STATE_LON, Double.NaN)
            if (CoordinateParser.isValid(lat, lon)) {
                selectPoint(GeoPoint(lat, lon), move = true)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        marker?.position?.let {
            outState.putDouble(STATE_LAT, it.latitude)
            outState.putDouble(STATE_LON, it.longitude)
        }
    }

    override fun onStart() {
        super.onStart()
        SimulationState.addListener(this)
        refreshUi()
    }

    override fun onStop() {
        SimulationState.removeListener(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        map.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        map.onDetach()
        super.onDestroy()
    }

    override fun onSimulationChanged() = refreshUi()

    // ---------------------------------------------------------------- Harita

    private fun initOsmdroid() {
        val config = Configuration.getInstance()
        config.load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        // OpenStreetMap karo politikası: tanımlayıcı User-Agent zorunludur.
        // Çatallıyorsanız bu değeri kendi uygulama adınız / iletişim bilginizle değiştirin.
        config.userAgentValue = USER_AGENT
        // Harici depolama izni gerektirmemesi için uygulamaya özel dizinler.
        config.osmdroidBasePath = File(filesDir, "osmdroid")
        config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.minZoomLevel = 3.0
        map.maxZoomLevel = 19.0
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.controller.setZoom(5.5)
        map.controller.setCenter(GeoPoint(39.0, 35.0))

        // "© OpenStreetMap contributors" atfı
        map.overlays.add(CopyrightOverlay(this))
        map.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p == null) return false
                    selectPoint(p, move = false)
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            })
        )
    }

    private fun selectPoint(point: GeoPoint, move: Boolean) {
        latInput.setText(CoordinateParser.format(point.latitude))
        lonInput.setText(CoordinateParser.format(point.longitude))
        latLayout.error = null
        lonLayout.error = null
        showMarker(point)
        if (move) {
            map.controller.setZoom(maxOf(map.zoomLevelDouble, 15.0))
            map.controller.animateTo(point)
        }
    }

    private fun showMarker(point: GeoPoint) {
        val current = marker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            it.title = getString(R.string.selected_marker)
            map.overlays.add(it)
            marker = it
        }
        current.position = point
        map.invalidate()
    }

    // ---------------------------------------------------------- Giriş alanları

    private fun readInput(): GeoPoint? {
        latLayout.error = null
        lonLayout.error = null
        val result = CoordinateParser.parse(
            latInput.text?.toString().orEmpty(),
            lonInput.text?.toString().orEmpty(),
        )
        return when (result) {
            is CoordinateParser.Result.Success -> GeoPoint(result.latitude, result.longitude)
            is CoordinateParser.Result.Failure -> {
                showProblem(result.problem)
                null
            }
        }
    }

    private fun showProblem(problem: CoordinateParser.Problem) {
        when (problem) {
            CoordinateParser.Problem.EMPTY_LAT -> latLayout.error = getString(R.string.err_lat_empty)
            CoordinateParser.Problem.EMPTY_LON -> lonLayout.error = getString(R.string.err_lon_empty)
            CoordinateParser.Problem.INVALID_LAT -> latLayout.error = getString(R.string.err_lat_invalid)
            CoordinateParser.Problem.INVALID_LON -> lonLayout.error = getString(R.string.err_lon_invalid)
            CoordinateParser.Problem.LAT_RANGE -> latLayout.error = getString(R.string.err_lat_range)
            CoordinateParser.Problem.LON_RANGE -> lonLayout.error = getString(R.string.err_lon_range)
        }
    }

    // --------------------------------------------------------------- Favoriler

    private fun addFavorite() {
        val point = readInput() ?: return
        val input = EditText(this).apply {
            hint = getString(R.string.favorite_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
            filters = arrayOf(InputFilter.LengthFilter(40))
        }
        val density = resources.displayMetrics.density
        val container = FrameLayout(this).apply {
            setPadding((20 * density).toInt(), (8 * density).toInt(), (20 * density).toInt(), 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.favorite_name_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim().ifEmpty {
                    getString(R.string.favorite_default_name, favorites.getAll().size + 1)
                }
                favorites.add(name, point.latitude, point.longitude)
                showMessage(getString(R.string.favorite_saved))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFavorites() {
        val list = favorites.getAll()
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.favorites_title)
                .setMessage(R.string.favorites_empty)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        val labels = list.map {
            "${it.name}\n${CoordinateParser.format(it.latitude)}, ${CoordinateParser.format(it.longitude)}"
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.favorites_title)
            .setItems(labels) { _, which -> showFavoriteActions(list[which]) }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showFavoriteActions(favorite: Favorite) {
        MaterialAlertDialogBuilder(this)
            .setTitle(favorite.name)
            .setMessage(
                "${CoordinateParser.format(favorite.latitude)}, ${CoordinateParser.format(favorite.longitude)}"
            )
            .setPositiveButton(R.string.favorite_select) { _, _ ->
                selectPoint(GeoPoint(favorite.latitude, favorite.longitude), move = true)
            }
            .setNegativeButton(R.string.favorite_delete) { _, _ ->
                favorites.remove(favorite.id)
                showMessage(getString(R.string.favorite_deleted))
            }
            .setNeutralButton(R.string.close, null)
            .show()
    }

    // -------------------------------------------------------------- Simülasyon

    private fun onToggleClicked() {
        if (SimulationState.running) {
            stopSimulationService()
            return
        }
        val point = readInput() ?: return
        selectPoint(point, move = false)
        requestStart(point)
    }

    private fun requestStart(point: GeoPoint) {
        if (hasLocationPermission()) {
            startSimulationService(point)
            return
        }
        pendingStart = point
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                permissionLauncher.launch(requiredPermissions())
            }
            .setNegativeButton(R.string.cancel) { _, _ -> pendingStart = null }
            .show()
    }

    private fun requiredPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startSimulationService(point: GeoPoint) {
        val intent = Intent(this, MockLocationService::class.java)
            .setAction(MockLocationService.ACTION_START)
            .putExtra(MockLocationService.EXTRA_LATITUDE, point.latitude)
            .putExtra(MockLocationService.EXTRA_LONGITUDE, point.longitude)
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            showError(getString(R.string.error_generic, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun stopSimulationService() {
        val intent = Intent(this, MockLocationService::class.java)
            .setAction(MockLocationService.ACTION_STOP)
        try {
            startService(intent)
        } catch (e: Exception) {
            showError(getString(R.string.error_generic, e.message ?: e.javaClass.simpleName))
        }
    }

    // --------------------------------------------------------------------- UI

    private fun refreshUi() {
        if (SimulationState.running) {
            statusText.text = getString(
                R.string.status_running,
                CoordinateParser.format(SimulationState.latitude),
                CoordinateParser.format(SimulationState.longitude),
            )
            statusText.setBackgroundColor(ContextCompat.getColor(this, R.color.green_container))
            toggleButton.setText(R.string.btn_stop)
            updateButton.visibility = View.VISIBLE
        } else {
            statusText.setText(R.string.status_idle)
            statusText.setBackgroundColor(ContextCompat.getColor(this, R.color.status_idle_bg))
            toggleButton.setText(R.string.btn_start)
            updateButton.visibility = View.GONE
        }
        SimulationState.consumeError()?.let { showError(it) }
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_setup) { _, _ -> openSetupGuide() }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun openSetupGuide() {
        startActivity(Intent(this, SetupGuideActivity::class.java))
    }

    private fun showMessage(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
    }

    private companion object {
        const val STATE_LAT = "state_lat"
        const val STATE_LON = "state_lon"
        const val USER_AGENT = "GeoPilot/0.1.0 (com.ymcgps.geopilot; acik kaynak konum test uygulamasi)"
    }
}
