package com.decagon.android.sq007.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.decagon.android.sq007.BuildConfig
import com.decagon.android.sq007.R
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.ui.email.TroubleSigningInFragment
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        /*Check if User is Already Signed In*/
        if (auth.currentUser != null) {
            // already signed in
            startMapActivity()
        } else {
            // not signed in
            createSignInIntent()
        }

    }

    private fun createSignInIntent() {
        // [START auth_fui_create_intent]
        // Select authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
//            ,AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                .setLogo(R.drawable.maps)
                .setTheme(R.style.LoginTheme)
                .build(),
            RC_SIGN_IN
        )
        // [END auth_fui_create_intent]
    }

    // [START auth_fui_result]
    @SuppressLint("LongLogTag")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                startMapActivity()
            } else {
                /*Sign in failed*/
                /*If response is null the user canceled the sign-in flow using the back button.*/
                if (response != null && response.error != null) {
                    val fullCredential = response.credentialForLinking
                    if (fullCredential != null) {

                        auth.signInWithCredential(fullCredential)
                            .addOnSuccessListener {
                                startMapActivity()
                            }
                    }
                } else if (response == null) {
                    // User pressed back button
                    Toast.makeText(this, "User Cancelled Sign-In", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                /*No Internet Connection*/
                else if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
                    return
                }
                /*Otherwise Get Error Type*/
                Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
                Log.e(TroubleSigningInFragment.TAG, "Sign-in error: ", response.error)
            }
        }
    }
    // [END auth_fui_result]

    /*Method to Navigate to Map Activity*/
    private fun startMapActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
        finish()
    }

    companion object {
        const val LOCATION_REQUEST_CODE = 101
        val auth = FirebaseAuth.getInstance()
        private const val RC_SIGN_IN = 123
    }

}