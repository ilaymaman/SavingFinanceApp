package com.example.savingfinance

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlin.math.log

class ActivityHome : ComponentActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupHomeLayout()// Start with login layout

        val username = intent.getStringExtra("USERNAME") ?: "User"

        // Find the TextView and set the welcome message
        val welcomeTextView = findViewById<TextView>(R.id.welcomeMessage) // Ensure this ID exists in your layout
        welcomeTextView.text = "Welcome, $username"


    }

    private fun setupHomeLayout() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.right_nav_view)

        // Set up navigation drawer button and listeners
        val openDrawerButton = findViewById<ImageButton>(R.id.open_drawer_button)
        openDrawerButton.setOnClickListener {
            Log.d("MainActivity", "opendrawer button clicked")
            drawerLayout.openDrawer(GravityCompat.END)

            val email = intent.getStringExtra("EMAIL")

            val headerTextView = findViewById<TextView>(R.id.Header_Username)
            headerTextView.text = "$email"
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemClick(menuItem)
            true
        }
    }

    // Handle navigation drawer item clicks
    private fun handleNavigationItemClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.profile -> {
                val intent = Intent(this, ActivityProfile::class.java)
                startActivity(intent)
            }
            R.id.settings -> {
                val intent = Intent(this, ActivitySettings::class.java)
                startActivity(intent)
            }
            R.id.logoutButton -> {
                val intent = Intent(this, ActivityLogin::class.java)
                startActivity(intent)
            }
        }

        // Close the drawer after handling the item click
        drawerLayout.closeDrawer(GravityCompat.END)
    }
}