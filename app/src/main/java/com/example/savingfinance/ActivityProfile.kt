package com.example.savingfinance

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        
        // Fetch user creation date
        fetchUserCreationDate()
    }
    
    private fun setupUserInfo() {
        // Display username and email
        findViewById<TextView>(R.id.profileUsername).text = username
        findViewById<TextView>(R.id.profileEmail).text = email
        findViewById<TextView>(R.id.profileUserId).text = userId
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
    
    private fun fetchUserCreationDate() {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val creationDate = document.getTimestamp("createdAt")
                    
                    if (creationDate != null) {
                        val formattedDate = formatDate(creationDate.toDate())
                        findViewById<TextView>(R.id.profileJoinDate).text = formattedDate
                    } else {
                        findViewById<TextView>(R.id.profileJoinDate).text = "Unknown"
                    }
                } else {
                    findViewById<TextView>(R.id.profileJoinDate).text = "Unknown"
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityProfile", "Error fetching user data", e)
                findViewById<TextView>(R.id.profileJoinDate).text = "Unknown"
            }
    }
    
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return format.format(date)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
