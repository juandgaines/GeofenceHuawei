package com.example.geofencedemojdgp

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geofencedemojdgp.GeofencingConstants.LANDMARK_DATA
import com.example.geofencedemojdgp.Utils.Companion.BACKGROUND_LOCATION_PERMISSION_INDEX
import com.example.geofencedemojdgp.Utils.Companion.LOCATION_PERMISSION_INDEX
import com.example.geofencedemojdgp.Utils.Companion.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
import com.example.geofencedemojdgp.Utils.Companion.REQUEST_TURN_DEVICE_LOCATION_ON
import com.example.geofencedemojdgp.Utils.Companion.checkDeviceLocationSettingsAndStartGeofence
import com.example.geofencedemojdgp.Utils.Companion.foregroundAndBackgroundLocationPermissionApproved
import com.example.geofencedemojdgp.Utils.Companion.isPermissionGranted
import com.example.geofencedemojdgp.Utils.Companion.requestForegroundAndBackgroundLocationPermissions
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.location.*
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.CircleOptions
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.util.LogM


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    //HUAWEI map


    private var hMap: HuaweiMap? = null
    private lateinit var mMapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var currentLocation: LatLng
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    lateinit var geofencingService: GeofenceService

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LogM.d(TAG, "onCreate:hzj")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //get mapview instance
        mMapView = findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mMapView.onCreate(mapViewBundle)
        //get map instance
        mMapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let { lr ->
                    val locations = lr.locations
                    if (locations.isNotEmpty()) {
                        locations.forEach { location ->
                            Log.d(
                                TAG,
                                "location-> long[${location.longitude}], lat[${location.latitude}]"
                            )
                        }
                    }
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
                locationAvailability?.let { la ->
                    val flag = locationAvailability.isLocationAvailable
                    Log.d(TAG, "onLocationAvailability isLocationAvailable:$flag")
                }
            }

        }

        checkPermissionsAndStartGeofencing()

        geofencingService = LocationServices.getGeofenceService(this)

        createChannel(this)

        val geofenceRequest = GeofenceRequest.Builder()
        geofenceRequest.createGeofenceList(GeofencingConstants.geofences)
        geofenceRequest.setCoordinateType(Geofence.ENTER_GEOFENCE_CONVERSION )
        val reuqest = geofenceRequest.build()
        val voidTask = geofencingService.createGeofenceList(reuqest, geofencePendingIntent)

        voidTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Geofences added succesfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Geofences adding fail ${it.exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun enableMyLocation() {
        if (isPermissionGranted(this)) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // If null call requestLocationUpdates in order to fetch it to cache
                    location?.let {
                        currentLocation = LatLng(location?.latitude!!, location.longitude)
                        focusMapOnCoordinates()
                    }

                }
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved(this, runningQOrLater)) {
            checkLocationSolver()
        } else {
            requestForegroundAndBackgroundLocationPermissions(
                this,
                runningQOrLater,
                this
            )
        }
    }

    private fun checkLocationSolver() {
        checkDeviceLocationSettingsAndStartGeofence(true, this, onSuccess = {
            requestLocationUpdatesWithCallback(it)
        }, onError = {
            it.startResolutionForResult(
                this@MainActivity,
                REQUEST_TURN_DEVICE_LOCATION_ON
            )
        })
    }

    private fun requestLocationUpdatesWithCallback(locationRequest: LocationRequest) {
        fusedLocationClient
            .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Log.i(
                    TAG, "requestLocationUpdatesWithCallback onSuccess"
                )
            })
            .addOnFailureListener { e ->
                Log.e(TAG, "requestLocationUpdatesWithCallback onFailure:" + e.message)
            }
    }

    override fun onMapReady(map: HuaweiMap) {
        //get map instance in a callback method
        Log.d(TAG, "onMapReady: ")
        hMap = map
        hMap?.clear()
        enableMyLocation()
        drawGeoFencesArea()


    }

    private fun drawGeoFencesArea() {

        LANDMARK_DATA.forEach {
            val geoFenceLatLong = LatLng(it.latitude, it.longitude)


            val circleOptions = CircleOptions()
                .center(
                    geoFenceLatLong
                )
                .radius(GeofencingConstants.GEOFENCE_RADIUS_IN_METERS.toDouble())
                .fillColor(0x4000ff00)
                .strokeColor(Color.GREEN)
                .strokeWidth(2f)
            hMap?.addCircle( circleOptions)
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        val removeTask =
            geofencingService.deleteGeofenceList(LANDMARK_DATA.map { it.identificator })
        removeTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Geofences removed succesfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Geofences adding fail ${it.exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // Permission denied.

            Log.d(TAG, "Permission denied")
            startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } else {
            checkDeviceLocationSettingsAndStartGeofence(true, this,
                onSuccess = {
                    requestLocationUpdatesWithCallback(it)
                },
                onError = {
                    it.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )

                })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false, this, onSuccess = {
                requestLocationUpdatesWithCallback(it)
                enableMyLocation()
            }, onError = {}
            )
        }
    }

    private fun focusMapOnCoordinates() {
        val zoomLevel = 18f
        hMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
        hMap?.isMyLocationEnabled = true

    }

    companion object {
        private const val TAG = "MainActivity"
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        internal const val ACTION_GEOFENCE_EVENT =
            "MapsActivity.wander.action.ACTION_GEOFENCE_EVENT"
    }
}
