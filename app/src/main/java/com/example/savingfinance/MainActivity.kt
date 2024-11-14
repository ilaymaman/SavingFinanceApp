package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    // Corrected method to open Signup Activity
    fun openSignupActivity(view: View) {
        // Use the class reference for the Signup activity
        val intent = Intent(this, ActivitySignup::class.java)
        startActivity(intent)
    }

    fun openLoginActivity(view: View) {
        val intent = Intent(this, ActivityLogin::class.java)
        startActivity(intent)
    }
}
