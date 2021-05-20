package com.decagon.android.sq007.model

data class Payload(
    val downloadUri: String,
    val fileId: String,
    val filename: String,
    val fileType: String,
    val uploadStatus: Boolean
)
