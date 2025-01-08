package com.example.savingfinance

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import kotlin.math.log

class MainActivity : ComponentActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_signup) // Start with login layout
    }

    // Existing methods
    fun openSignupActivity(view: View) {
        val intent = Intent(this, ActivitySignup::class.java)
        startActivity(intent)
    }

    fun openLoginActivity(view: View) {
        val intent = Intent(this, ActivityLogin::class.java)
        startActivity(intent)
    }

    fun openHomeActivity(view: View) {
        val intent = Intent(this, ActivityHome::class.java)
        startActivity(intent)
    }
}