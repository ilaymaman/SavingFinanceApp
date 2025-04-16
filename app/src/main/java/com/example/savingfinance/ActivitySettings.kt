package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ActivitySettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val currencyLayout = findViewById<LinearLayout>(R.id.currencyPreferenceLayout)
        val backupLayout = findViewById<LinearLayout>(R.id.backupRestoreLayout)
        val profileLayout = findViewById<LinearLayout>(R.id.profileLayout) // Add ID to profile layout first
        val aboutLayout = findViewById<LinearLayout>(R.id.aboutLayout) // Add ID to about layout first

        // Set up click listeners for settings options
        currencyLayout.setOnClickListener {
            Toast.makeText(this, "Currency settings coming soon", Toast.LENGTH_SHORT).show()
            // Future implementation: open currency selection dialog
        }

        backupLayout.setOnClickListener {
            Toast.makeText(this, "Backup & Restore coming soon", Toast.LENGTH_SHORT).show()
            // Future implementation: backup/restore functionality
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
    }

    // Also handle the system back button
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
