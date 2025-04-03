package com.example.savingfinance

import android.annotation.SuppressLint
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class ActivityHome : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupHomeLayout()// Start with login layout

        firestore = FirebaseFirestore.getInstance()

        username = intent.getStringExtra("USERNAME") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: "Guest"
        email = intent.getStringExtra("EMAIL") ?: "No Email"

        loadFragment(TransactionFragment.newInstance(userId))

        // Find the TextView and set the welcome message
        val welcomeTextView = findViewById<TextView>(R.id.welcomeMessage) // Ensure this ID exists in your layout
        welcomeTextView.text = "Welcome, $username"

        if (userId.isNotEmpty()) {
            fetchGoals()
        } else {
            Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show()
        }

        val AddButton = findViewById<ImageButton>(R.id.addButton)
        AddButton.setOnClickListener {
            val bottomSheet = AddActivityBottomSheet()
            bottomSheet.show(supportFragmentManager, "AddActivityBottomSheet")
        }

        val transactionsButton = findViewById<Button>(R.id.transactionsButton)
        val goalsButton = findViewById<Button>(R.id.goalsButton)

        transactionsButton.setOnClickListener {
            loadFragment(TransactionFragment.newInstance(userId))
        }

        goalsButton.setOnClickListener {
            loadFragment(GoalsFragment.newInstance(userId))
        }
    }

    fun updateMainGoalDisplay(goalName: String, currentAmount: Int, goalAmount: Int) {
        findViewById<TextView>(R.id.savingAmount).text = if (goalAmount > 0) "$$currentAmount" else ""
        findViewById<TextView>(R.id.savingGoal).text = if (goalAmount > 0) "of your $$goalAmount saving goal" else ""
        findViewById<TextView>(R.id.savingTrackerTitle).text = goalName
        
        findViewById<ProgressBar>(R.id.savingProgress).apply {
            max = if (goalAmount > 0) goalAmount else 100
            progress = if (goalAmount > 0) currentAmount else 0
            visibility = if (goalAmount > 0) View.VISIBLE else View.GONE
        }
    }

    private fun fetchGoals() {
        firestore.collection("users").document(userId)
            .collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                // Find the main goal
                val mainGoal = documents.find { it.getBoolean("isMainGoal") == true }
                
                if (mainGoal != null) {
                    val goalName = mainGoal.getString("name") ?: "Main Goal"
                    val currentAmount = mainGoal.getDouble("currentAmount")?.toInt() ?: 0
                    val goalAmount = mainGoal.getDouble("goalAmount")?.toInt() ?: 0
                    
                    updateMainGoalDisplay(goalName, currentAmount, goalAmount)
                } else {
                    // No main goal found
                    updateMainGoalDisplay("No main goal was set yet", 0, 0)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityHome", "Error fetching goals", e)
                // Show default message on error
                updateMainGoalDisplay("No main goal was set yet", 0, 0)
            }
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

    fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}