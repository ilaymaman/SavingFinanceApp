package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    // Corrected method to open Signup Activity
    fun openSignupActivity(view: View) {
        val intent = Intent(this, ActivitySignup::class.java)
        setContentView(R.layout.activity_signup)

    }

    fun openLoginActivity(view: View) {
        val intent = Intent(this, ActivityLogin::class.java)
        setContentView(R.layout.activity_login)
    }
}
