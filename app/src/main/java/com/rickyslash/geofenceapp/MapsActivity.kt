package com.rickyslash.geofenceapp

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rickyslash.geofenceapp.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val centerLat = -8.179256984676815
    private val centerLng = 113.68787503507323
    private val geofenceRadius = 400.0

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private lateinit var geofencingClient: GeofencingClient

    // this to send broadcast to GeofenceBroadcastReceiver
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    // ActivityResultContracts variable to request `both` permission
    @TargetApi(Build.VERSION_CODES.Q)
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (runningQOrLater) {
                    // request permission of ACCESS_FINE_LOCATION by requestBackgroundLocationPermissionLauncher ActivityResultContracts variable
                    requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    getMyLocation()
                }
            }
        }

    // ActivityResultContracts variable to get request for `ACCESS_BACKGROUND_LOCATION` permission (for Android Q)
    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true

        val kulinerLegenda = LatLng(centerLat, centerLng)
        mMap.addMarker(MarkerOptions().position(kulinerLegenda).title("Kuliner Gudeg Pecel Legendaris"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kulinerLegenda, 15f))

        // add circle for geofencing
        mMap.addCircle(
            CircleOptions()
                .center(kulinerLegenda)
                .radius(geofenceRadius)
                .fillColor(0x22FF0000)
                .strokeColor(Color.RED)
                .strokeWidth(3f)
        )

        getMyLocation()
        addGeofence()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        // initializing geofence client
        geofencingClient = LocationServices.getGeofencingClient(this)

        // making Geofence object
        val geofence = Geofence.Builder()
            .setRequestId("Gudeg Legendaris")
            .setCircularRegion(
                centerLat,
                centerLng,
                geofenceRadius.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // set what kind of transition that triggers geofence
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            // set time in millisecond that user need to be inside geofence before triggered
            .setLoiteringDelay(5000)
            .build()

        // building GeofencingRequest object
        val geofencingRequest = GeofencingRequest.Builder()
            // Geofence event immediately run when user inside geofence because of the setInitialTrigger INITIAL_TRIGGER_ENTER
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // remove existing Geofences associated with geofencePendingIntent
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                // add new geofences after successful Geofence remove with geofencePendingIntent
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener { showToast("Geofencing added") }
                }
                addOnFailureListener {
                    showToast("Geofencing failed: ${it.message}")
                }
            }
        }
    }

    // get location when permission is fulfilled
    private fun getMyLocation() {
        if (checkForegroundAndBackgroundLocationPermission()) {
            mMap.isMyLocationEnabled = true
        } else {
            // request permission of ACCESS_FINE_LOCATION by requestLocationPermissionLauncher ActivityResultContracts variable
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // checks Foreground & Background permission
    @TargetApi(Build.VERSION_CODES.Q)
    private fun checkForegroundAndBackgroundLocationPermission(): Boolean {
        val foregroundLocationApproved = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved = if (runningQOrLater) {
            checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    // check permission whether it's granted or not
    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(text: String) {
        Toast.makeText(this@MapsActivity, text, Toast.LENGTH_SHORT).show()
    }


}