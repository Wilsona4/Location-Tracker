package com.decagon.android.sq007.model

import com.google.firebase.database.Exclude

data class Locations(
    @get:Exclude
    var id: String? = null,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0
)
