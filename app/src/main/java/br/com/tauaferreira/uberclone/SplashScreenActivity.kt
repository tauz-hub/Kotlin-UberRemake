package br.com.tauaferreira.uberclone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import br.com.tauaferreira.uberclone.Common.DRIVER_INFO_REFERENCE
import br.com.tauaferreira.uberclone.Model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference

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
        Completable.timer(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()
    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(DRIVER_INFO_REFERENCE)

        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null) {
                checkFromUserInDatabase()
            } else {
                showLoginLayout()
            }
        }
    }

    private fun checkFromUserInDatabase() {
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "User already register!",
                            Toast.LENGTH_SHORT
                        ).show()


                    } else {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT)
                        .show()
                }

            })

    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edt_first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number =
            itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val progress_bar = findViewById<ProgressBar>(R.id.progress_bar) as ProgressBar
        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        //set Data
        if (
            FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        ) {
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }

        //view
        builder.setView(itemView)

        val dialog = builder.create()
        dialog.show()

        //event
        btn_continue.setOnClickListener() {
            if (TextUtils.isDigitsOnly(edt_first_name.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please enter First Name",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else if (TextUtils.isDigitsOnly(edt_last_name.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please enter Last Name",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else if (TextUtils.isDigitsOnly(edt_phone_number.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please enter Phone Number",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else {
                val model = DriverInfoModel()

                model.firstName = edt_first_name.text.toString()
                model.lastName = edt_last_name.text.toString()
                model.phoneNumber = edt_phone_number.text.toString()
                model.rating = 0.0



                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "Register Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()

                        progress_bar.visibility = View.GONE
                    }
            }
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

    private
    val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

            val response = IdpResponse.fromResultIntent(it.data)
            if (it.resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "" + response!!.error!!.message,
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
}