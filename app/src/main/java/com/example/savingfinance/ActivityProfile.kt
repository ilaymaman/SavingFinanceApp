package com.example.savingfinance

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ActivityProfile : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()
        
        // Get data from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""
        email = intent.getStringExtra("EMAIL") ?: ""
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Set user information
        setupUserInfo()
        
        // Fetch and display user statistics
        fetchUserStatistics()
    }
    
    private fun setupUserInfo() {
        // Display username and email
        findViewById<TextView>(R.id.profileUsername).text = username
        findViewById<TextView>(R.id.profileEmail).text = email
        findViewById<TextView>(R.id.profileEmailDetail).text = email
    }
    
    private fun fetchUserStatistics() {
        // Fetch transaction count
        fetchTransactionCount()
        
        // Fetch goal count
        fetchGoalCount()
    }
    
    private fun fetchTransactionCount() {
        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                findViewById<TextView>(R.id.transactionCount).text = count.toString()
            }
            .addOnFailureListener { e ->
                Log.e("ActivityProfile", "Error fetching transactions", e)
                Toast.makeText(this, "Error loading transactions: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun fetchGoalCount() {
        firestore.collection("users").document(userId)
            .collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                findViewById<TextView>(R.id.goalCount).text = count.toString()
            }
            .addOnFailureListener { e ->
                Log.e("ActivityProfile", "Error fetching goals", e)
                Toast.makeText(this, "Error loading goals: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
