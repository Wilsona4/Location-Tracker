package com.decagon.android.sq007.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.decagon.android.sq007.R
import com.decagon.android.sq007.databinding.ActivityMapsBinding
import com.decagon.android.sq007.model.Locations
import com.decagon.android.sq007.view.MainActivity.Companion.LOCATION_REQUEST_CODE
import com.decagon.android.sq007.viewModel.LocationViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMyLocationButtonClickListener, OnMyLocationClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding


    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var viewModel: LocationViewModel
    private lateinit var locationList: MutableList<Locations>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        setContentView(binding.root)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    /*Manipulates the map once available.*/
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            makePermissionRequest()
            return
        }

        mMap.uiSettings?.isZoomControlsEnabled = true
        mMap.uiSettings?.setAllGesturesEnabled(true)
        mMap.uiSettings?.isCompassEnabled = true

        mMap.isMyLocationEnabled = true
        mMap.uiSettings?.isMyLocationButtonEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)


        // Get the current location of the device and set the position of the map.
        createLocationRequest()
        getDeviceLocation()

    }

    /*Get the best and most recent location of the device, which may be null in rare cases when a location is not available.*/
    private fun getDeviceLocation() {

        try {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
                /*Observe Location List LiveData*/
                viewModel.locationStatus.observe(
                    this@MapsActivity, Observer {
                        locationList = it.toMutableList()
                        for (i in locationList.indices) {
                            val retrievedLatitude = locationList[i].latitude
                            val retrievedLongitude = locationList[i].longitude
                            val retrievedId = locationList[i].id
                            Log.d("KEYS", "$retrievedId")

                            if (retrievedLatitude != null && retrievedLongitude != null) {
                                val position = LatLng(retrievedLatitude, retrievedLongitude)

                                when (retrievedId) {
                                    "Wilson Ahanmisi" -> mMap.addMarker(
                                        MarkerOptions()
                                            .position(position)
                                            .title(retrievedId)
                                            .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_BLUE))
                                    )
                                    else -> mMap.addMarker(
                                        MarkerOptions()
                                            .position(position)
                                            .title(retrievedId)
                                    )
                                }


                            } else {
                                Log.d(TAG, "Current location is null. Using defaults.")
                                mMap.moveCamera(
                                    CameraUpdateFactory
                                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                                )
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(defaultLocation)
                                        .title("Sydney")

                                )
                                mMap.isMyLocationEnabled = false
                                mMap.uiSettings?.isMyLocationButtonEnabled = false
                            }
                        }
                    }
                )
                viewModel.displayLocationChanges()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {

                    val location = locationResult.lastLocation
                    if (location != null) {
                        mMap.clear()

                        val myLocation = Locations(MR_WIL, location.latitude, location.longitude)
                        /*Add to FireBase*/
                        viewModel.updateLocation(myLocation)

                        /*Check if Data Uploaded Successfully*/
                        viewModel.uploadStatus.observe(
                            this@MapsActivity,
                            {
                                if (it != null) {
                                    Toast.makeText(
                                        applicationContext,
                                        "${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }
//                                else {
//                                    Toast.makeText(applicationContext, "Location Update Successfully", Toast.LENGTH_SHORT).show()
//                                }
                            }
                        )
                    }
                }
            }
        }
    }

    /*This function enables the user to change the map type*/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        R.id.gallery -> {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.mSignOut -> {
            Toast.makeText(this, "Bye-Bye", Toast.LENGTH_SHORT).show()
            signOut()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /*Make Permission Request*/
    private fun makePermissionRequest() {

        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }

    }

    /* Handles the result of the request for location permissions.*/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(p0: Location) {
    }

    /*Function to Sign Out*/
    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
        finish()
    }

    override fun onResume() {
        super.onResume()
        createLocationRequest()
        getDeviceLocation()
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15

        const val MR_WIL = "wilsonahanmisi@gmail.com"
    }

}