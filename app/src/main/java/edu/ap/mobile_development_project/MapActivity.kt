package edu.ap.mobile_development_project

import android.app.Activity
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.ap.mobile_development_project.services.FilterService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

class MapActivity : Activity() {
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var mMapView: MapView
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()
    private var currentLocationOverlay: MyLocationNewOverlay? = null

    private var startLatitude: Double? = null
    private var startLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val extras = intent.extras
        if (extras != null) {
            startLatitude = extras.getString("latitude")?.toDouble()
            startLongitude = extras.getString("longitude")?.toDouble()
        }

        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        mMapView = findViewById(R.id.map_view)
        initMap()

        // Navbar things
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.map
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.list -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.map -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun initMap() {
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        val provider = GpsMyLocationProvider(this)
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER)
        currentLocationOverlay = MyLocationNewOverlay(mMapView)
        mMapView.overlays?.add(currentLocationOverlay)

        val toiletList = FilterService.instance.getAll()
        for (t in toiletList) {
            val geoPoint = GeoPoint(t.latitude!!, t.longitude!!)
            addMarker(geoPoint, t.id!!.toBigDecimal().toPlainString())
        }

        val startPosition = GeoPoint(startLatitude ?: 51.23020595, startLongitude ?: 4.41655480828479)
        mMapView.controller?.setCenter(startPosition)
        val zoom = if (startLatitude == null) 14 else 16
        mMapView.controller?.setZoom(zoom)
    }

    private fun addMarker(geoPoint: GeoPoint, name: String) {
        items.add(OverlayItem(name, name, geoPoint))
        mMyLocationOverlay = ItemizedIconOverlay(items, null, applicationContext)
        mMapView.overlays?.add(mMyLocationOverlay)
    }

    override fun onPause() {
        super.onPause()
        mMapView.onPause()
        currentLocationOverlay?.disableMyLocation()
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
        currentLocationOverlay?.enableMyLocation()
    }
}