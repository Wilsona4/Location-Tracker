package com.decagon.android.sq007.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentResolverCompat.query
import androidx.core.content.ContextCompat
import com.decagon.android.sq007.api.RetrofitInstance
import com.decagon.android.sq007.databinding.ActivityGalleryBinding
import com.decagon.android.sq007.model.PhotoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GalleryActivity : AppCompatActivity() {
    lateinit var binding: ActivityGalleryBinding
    private lateinit var imageUri: Uri
    private lateinit var downloadLink: String
    private lateinit var picture: Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivGetImage.setOnClickListener {
            if (isPermissionGranted()) {
                pickFromGallery()
            } else {
                askForPermissions()
            }
        }

        // upload image
        binding.btnUpload.setOnClickListener {
            uploadImage(imageUri)
        }
        // download image
        binding.btnDownload.setOnClickListener {
            downloadPhoto()
        }
    }

    /*check for permission*/
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*Ask For Permission*/
    private fun askForPermissions(): Boolean {
        if (!isPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this as Activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showPermissionDeniedDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this as Activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    GALLERY_REQUEST_CODE
                )
            }
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            GALLERY_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission is granted, you can perform your operation here
                    pickFromGallery()
                } else {
                    // permission is denied, you can ask for permission again, if you want
                    askForPermissions()
                }
                return
            }
        }
    }

    // show runtime permission dialogue
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Permission is denied, Please allow permissions from App Settings.")
            .setPositiveButton(
                "App Settings"
            ) { dialogInterface, i ->
                // send to app settings if permission is denied permanently
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /*Select Image From Gallery*/
    @SuppressLint("QueryPermissionsNeeded")
    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "RESULT OK")
            when (requestCode) {

                GALLERY_REQUEST_CODE -> {
                    Log.d(TAG, "GALLERY_REQUEST_CODE Detected.")
                    data?.data?.let { uri: Uri ->
                        Log.d(TAG, "URI: $uri")
                        // display picture in holder
                        picture = binding.ivRetrievedImage.setImageURI(data.data)
                        // get image uri
                        imageUri = data.data!!
                        Log.d(TAG, "Upload uri: $imageUri")
                    }
                }
            }
        }
    }

    /*Upload Image*/
    private fun uploadImage(uri: Uri) {
        val parcelFileDescriptor = this.contentResolver.openFileDescriptor(uri, "r", null) ?: return
        // get file details
        val file = File(this.cacheDir, getFileName(uri, this.contentResolver))
        //
        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val outStream = FileOutputStream(file)
        inputStream.copyTo(outStream)
        //
        val body = file.asRequestBody("image/png".toMediaTypeOrNull())
        // get image information to upload
        val image = MultipartBody.Part.createFormData("file", file.name, body)
        // initiate upload with retrofit
        RetrofitInstance.photoUploadApi.uploadImage(image)
            .enqueue(object : Callback<PhotoFormat> {
                override fun onResponse(call: Call<PhotoFormat>, response: Response<PhotoFormat>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@GalleryActivity,
                            "Upload Successful",
                            Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, "output success: ${response.body().toString()}")
                        // get the download link
                        downloadLink = response.body()?.payload?.downloadUri ?: String()
                        // clear the image holder
                        binding.ivRetrievedImage.setImageResource(0)
                    } else {
                        Toast.makeText(this@GalleryActivity,
                            "Uplaod not Successful",
                            Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "output is $response")
                    }
                }

                override fun onFailure(call: Call<PhotoFormat>, t: Throwable) {
                    Toast.makeText(this@GalleryActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "output is ${t.message}")
                }
            }
            )
    }

    // get file name
    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        var name = ""
        val cursor = query(contentResolver, uri, null, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            name = cursor.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }

    // function to download image
    private fun downloadPhoto() {
        // set dispatchers
        GlobalScope.launch(context = Dispatchers.IO) {
            // save the reference in a variable
            val imageLink = URL(downloadLink)
            // open connection to the website
            val connection = imageLink.openConnection() as HttpURLConnection
            // initiate input stream
            connection.doInput = true
            connection.connect()
            // save data coming from internet
            val inputStream: InputStream? = connection.inputStream
            // decode input to bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)
            // return result to main thread
            launch(context = Main) {
                binding.ivRetrievedImage.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        private val TAG = GalleryActivity::class.java.simpleName
        private const val GALLERY_REQUEST_CODE = 15

    }
}
