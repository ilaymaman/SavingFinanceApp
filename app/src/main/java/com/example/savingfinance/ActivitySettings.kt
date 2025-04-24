package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ActivitySettings : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var currencyValueText: TextView
    private lateinit var firestore: FirebaseFirestore
    private var currentCurrencySymbol = "$"
    private var currentCurrencyCode = "USD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        firestore = FirebaseFirestore.getInstance()

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val currencyLayout = findViewById<LinearLayout>(R.id.currencyPreferenceLayout)
        val profileLayout = findViewById<LinearLayout>(R.id.profileLayout)
        currencyValueText = findViewById(R.id.currencyValue)

        // Get data from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""
        email = intent.getStringExtra("EMAIL") ?: ""

        // Get the user's current currency
        if (userId.isNotEmpty()) {
            fetchUserCurrency()
        }

        // Set up click listeners for settings options
        currencyLayout.setOnClickListener {
            showCurrencySelector()
        }

        // Handle back button click
        backButton.setOnClickListener {
            returnToPreviousScreen() // Use the same method for consistency
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

    private fun fetchUserCurrency() {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val currency = document.getString("preferredCurrency") ?: "$"
                    currentCurrencySymbol = currency
                    currentCurrencyCode = if (currency == "$") "USD" else "NIS"
                    updateCurrencyDisplay()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user preferences", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCurrencyDisplay() {
        currencyValueText.text = if (currentCurrencySymbol == "$") "$ USD" else "₪ NIS"
    }

    private fun showCurrencySelector() {
        val bottomSheet = CurrencyBottomSheet(userId) { symbol, code ->
            currentCurrencySymbol = symbol
            currentCurrencyCode = code
            updateCurrencyDisplay()
        }
        bottomSheet.show(supportFragmentManager, "CurrencyBottomSheet")
    }

    // Also handle the system back button
    override fun onBackPressed() {
        super.onBackPressed()
        returnToPreviousScreen()
    }

    private fun returnToPreviousScreen() {
        // Pass the currency back to the previous activity
        val resultIntent = Intent()
        resultIntent.putExtra("CURRENCY_SYMBOL", currentCurrencySymbol)
        resultIntent.putExtra("CURRENCY_CODE", currentCurrencyCode)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
