package com.decagon.android.sq007.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.decagon.android.sq007.model.Locations
import com.decagon.android.sq007.view.MapsActivity.Companion.MR_WIL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LocationViewModel : ViewModel() {

    private val dbLocationsRef = Firebase.database.reference
    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    /*Variable to store upload Status (success/failure)*/
    private var _uploadStatus = MutableLiveData<Exception?>()
    val uploadStatus: LiveData<Exception?> get() = _uploadStatus

    /*Variable to Listen for Location Change*/
    private var _locationStatus = MutableLiveData<List<Locations>>()
    val locationStatus: LiveData<List<Locations>> get() = _locationStatus


    /*Create a Database Event Listener*/
    private val locationEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val locations = mutableListOf<Locations>()
            if (snapshot.exists()) {
                for (locationSnapshot in snapshot.children) {
                    val location = locationSnapshot.getValue(Locations::class.java)

                    location?.id = locationSnapshot.key.toString()
                    location?.let { locations.add(it) }
                }
            } else {
                locations.clear()
            }
            _locationStatus.value = locations
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }

    /*Get Real-Time Changes in the Database*/
    fun displayLocationChanges() {
        dbLocationsRef.addValueEventListener(locationEventListener)
    }

    /*Update Contact in Database*/
    fun updateLocation(location: Locations) {
        user?.let {
            dbLocationsRef.child(user.displayName?: user.uid).setValue(location)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        _uploadStatus.value = null
                    } else {
                        _uploadStatus.value = it.exception
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        dbLocationsRef.removeEventListener(locationEventListener)
    }

}