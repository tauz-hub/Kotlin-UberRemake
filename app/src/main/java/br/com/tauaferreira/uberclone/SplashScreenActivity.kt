package br.com.tauaferreira.uberclone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private val LOGIN_REQUEST_CODE = 7171

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
       firebaseAuth.removeAuthStateListener { listener }
        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null)
                Toast.makeText(this@SplashScreenActivity, "Welcome " + user.uid, Toast.LENGTH_SHORT)
                    .show()
            else
                showLoginLayout()
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()


        val instaceOfAuth = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        getResult.launch(instaceOfAuth)
    }
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

            if (it.resultCode == LOGIN_REQUEST_CODE) {
                val response = IdpResponse.fromResultIntent(it.data)
                if (it.resultCode == Activity.RESULT_OK) {
                    val user = FirebaseAuth.getInstance().currentUser
                } else {
                    Toast.makeText(this@SplashScreenActivity, ""+ response!!.error!!.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
}