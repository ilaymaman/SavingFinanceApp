package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class ActivitySettings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val logoutButton = findViewById<Button>(R.id.logoutButton)

        logoutButton.setOnClickListener{
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }
    }
}
