package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class ActivitySettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Handle back button click
        backButton.setOnClickListener {
            finish() // Close the settings activity and return to previous screen
        }

        logoutButton.setOnClickListener {
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
            finishAffinity() // Close all activities in the stack
        }
    }

    // Also handle the system back button
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
