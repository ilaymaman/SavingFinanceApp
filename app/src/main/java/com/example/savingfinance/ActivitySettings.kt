package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ActivitySettings : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val currencyLayout = findViewById<LinearLayout>(R.id.currencyPreferenceLayout)
        val profileLayout = findViewById<LinearLayout>(R.id.profileLayout) // Add ID to profile layout first

        // Get data from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""
        email = intent.getStringExtra("EMAIL") ?: ""

        // Set up click listeners for settings options
        currencyLayout.setOnClickListener {
            Toast.makeText(this, "Currency settings coming soon", Toast.LENGTH_SHORT).show()
            // Future implementation: open currency selection dialog
        }

        // Handle back button click
        backButton.setOnClickListener {
            finish() // Close the settings activity and return to previous screen
        }

        logoutButton.setOnClickListener {
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
            finishAffinity() // Close all activities in the stack
        }

        profileLayout.setOnClickListener {
            val intent = Intent(this, ActivityProfile::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("USERNAME", username)
            intent.putExtra("EMAIL", email)
            startActivity(intent)
        }
    }

    // Also handle the system back button
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
