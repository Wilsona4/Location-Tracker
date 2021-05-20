package com.decagon.android.sq007.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://darot-image-upload-service.herokuapp.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val photoUploadApi: PhotoUploadApi by lazy {
        retrofit.create(PhotoUploadApi::class.java)
    }
}
