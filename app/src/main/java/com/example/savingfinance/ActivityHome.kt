package com.example.savingfinance

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class ActivityHome : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var preferredCurrency: String
    private lateinit var drawerLayout: DrawerLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // First set the content view
            setContentView(R.layout.activity_home)

            // Get user data from intent with defaults to prevent null crashes
            username = intent.getStringExtra("USERNAME") ?: "User"
            userId = intent.getStringExtra("USER_ID") ?: ""
            email = intent.getStringExtra("EMAIL") ?: ""
            preferredCurrency = intent.getStringExtra("CURRENCY") ?: "$"

            // Initialize Firebase
            firestore = FirebaseFirestore.getInstance()

            // Setup UI components in a specific order
            setupWelcomeMessage() // Setup welcome message first
            setupDrawer() // Setup drawer second
            setupButtons() // Setup buttons third
            
            // Only try to load fragment and fetch goals if we have a valid user ID
            if (userId.isNotEmpty()) {
                // First set a default display for goals
                updateMainGoalDisplay("Loading...", 0, 0)
                
                // Then try to load the transactions fragment
                loadFragment(TransactionFragment.newInstance(userId))
                
                // Finally fetch goals
                fetchGoals()
            } else {
                Toast.makeText(this, "Missing user ID - some features may not work", Toast.LENGTH_LONG).show()
                updateMainGoalDisplay("Please log in", 0, 0)
            }
        } catch (e: Exception) {
            Log.e("ActivityHome", "Fatal error in onCreate", e)
            Toast.makeText(this, "Failed to start the app: ${e.localizedMessage}", Toast.LENGTH_LONG).show()

            // If there's a fatal error, go back to login
            try {
                val intent = Intent(this, ActivityLogin::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e2: Exception) {
                // Nothing more we can do
            }
        }
    }

    private fun setupDrawer() {
        try {
            // Initialize drawer
            drawerLayout = findViewById(R.id.drawer_layout)
            
            // Set username in header
            val headerUsernameTextView = findViewById<TextView>(R.id.Header_Username)
            headerUsernameTextView.text = username
            
            // Setup menu button
            val openDrawerButton = findViewById<ImageButton>(R.id.open_drawer_button)
            openDrawerButton.setOnClickListener {
                try {
                    drawerLayout.openDrawer(GravityCompat.END)
                } catch (e: Exception) {
                    handleError(e, "Error opening drawer")
                }
            }

            // Set click listeners for navigation items
            val profileMenuItem = findViewById<LinearLayout>(R.id.profile_menu_item)
            val settingsMenuItem = findViewById<LinearLayout>(R.id.settings_menu_item)
            val logoutMenuItem = findViewById<LinearLayout>(R.id.logout_menu_item)
            
            profileMenuItem.setOnClickListener {
                // Close drawer
                drawerLayout.closeDrawer(GravityCompat.END)
                
                // Navigate to Profile activity
                val intent = Intent(this, ActivityProfile::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("USERNAME", username)
                intent.putExtra("EMAIL", email)
                startActivity(intent)
            }
            
            settingsMenuItem.setOnClickListener {
                try {
                    val intent = Intent(this, ActivitySettings::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("USERNAME", username)
                    intent.putExtra("EMAIL", email)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.END)
                } catch (e: Exception) {
                    handleError(e, "Error navigating to settings")
                }
            }
            
            logoutMenuItem.setOnClickListener {
                try {
                    val intent = Intent(this, ActivityLogin::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    handleError(e, "Error logging out")
                }
            }
        } catch (e: Exception) {
            handleError(e, "Error setting up drawer")
        }
    }

    private fun setupWelcomeMessage() {
        try {
            val welcomeTextView = findViewById<TextView>(R.id.welcomeMessage)
            welcomeTextView.text = "Welcome, $username"
        } catch (e: Exception) {
            handleError(e, "Error setting welcome message")
        }
    }

    private fun setupButtons() {
        try {
            // Add button for bottom sheet
            val addButton = findViewById<ImageButton>(R.id.addButton)
            addButton.setOnClickListener {
                try {
                    val bottomSheet = AddActivityBottomSheet()
                    bottomSheet.show(supportFragmentManager, "AddActivityBottomSheet")
                } catch (e: Exception) {
                    handleError(e, "Error showing bottom sheet")
                }
            }

            // Tab buttons
            val transactionsButton = findViewById<Button>(R.id.transactionsButton)
            val goalsButton = findViewById<Button>(R.id.goalsButton)

            transactionsButton.setOnClickListener {
                try {
                    loadFragment(TransactionFragment.newInstance(userId))
                } catch (e: Exception) {
                    handleError(e, "Error loading transactions")
                }
            }

            goalsButton.setOnClickListener {
                try {
                    loadFragment(GoalsFragment.newInstance(userId))
                } catch (e: Exception) {
                    handleError(e, "Error loading goals")
                }
            }
        } catch (e: Exception) {
            handleError(e, "Error setting up buttons")
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    fun updateMainGoalDisplay(goalName: String, currentAmount: Int, goalAmount: Int) {
        try {
            val savingAmountView = findViewById<TextView>(R.id.savingAmount)
            val savingGoalView = findViewById<TextView>(R.id.savingGoal)
            val savingTrackerTitleView = findViewById<TextView>(R.id.savingTrackerTitle)
            val progressBarView = findViewById<ProgressBar>(R.id.savingProgress)
            
            savingAmountView.text = if (goalAmount > 0) "$preferredCurrency$currentAmount" else "$0"
            savingGoalView.text = if (goalAmount > 0) "of your $preferredCurrency$goalAmount saving goal" else "No goal set"
            savingTrackerTitleView.text = goalName
            
            progressBarView.apply {
                max = if (goalAmount > 0) goalAmount else 100
                progress = if (goalAmount > 0) currentAmount else 0
                visibility = if (goalAmount > 0) View.VISIBLE else View.VISIBLE
            }
        } catch (e: Exception) {
            handleError(e, "Error updating goal display")
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

    private fun handleError(error: Exception, message: String) {
        Log.e("ActivityHome", message, error)
        Toast.makeText(this, "Something went wrong: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

    fun loadFragment(fragment: Fragment) {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.commit()
        } catch (e: Exception) {
            handleError(e, "Error loading fragment")
        }
    }
}