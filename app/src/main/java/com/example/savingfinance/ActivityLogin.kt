package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.savingfinance.ActivitySignup.Companion.TAG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLogin : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loginButton: Button
    private val timeoutDuration = 15000L // 15 seconds timeout
    private val handler = Handler(Looper.getMainLooper())
    private var isAuthInProgress = false
    private var showLoadingDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Setup UI
        setContentView(R.layout.activity_login)
        loginButton = findViewById(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signUpText)
        
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //reload()
        }

        loginButton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.emailEditText)
            val passwordInput = findViewById<EditText>(R.id.passwordEditText)
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signIn(email, password)
            } else {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, ActivitySignup::class.java)
            startActivity(intent)
        }
    }

    private fun signIn(email: String, password: String) {
        if (isAuthInProgress) return
        
        isAuthInProgress = true
        loginButton.isEnabled = false
        
        // Set timeout for login
        val timeoutRunnable = Runnable {
            if (isAuthInProgress) {
                Toast.makeText(this, "Login timed out. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                isAuthInProgress = false
                loginButton.isEnabled = true
            }
        }
        
        handler.postDelayed(timeoutRunnable, timeoutDuration)
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                handler.removeCallbacks(timeoutRunnable)
                
                if (!isAuthInProgress) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    fetchUsernameAndUpdateUI(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    
                    val errorMessage = when(task.exception) {
                        is FirebaseAuthInvalidUserException -> "No account exists with this email. Please sign up."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> "Authentication failed. ${task.exception?.message ?: "Please try again."}"
                    }
                    
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                    isAuthInProgress = false
                    loginButton.isEnabled = true
                }
            }
    }

    private fun fetchUsernameAndUpdateUI(user: FirebaseUser?) {
        if (user != null) {
            val userId = user.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (!isAuthInProgress) return@addOnSuccessListener
                    

                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "User"
                        val email = document.getString("email") ?: "User"
                        val preferredCurrency = document.getString("preferredCurrency") ?: "$"
                        updateUI(user, username, email, preferredCurrency)
                    } else {
                        Log.w(TAG, "No such document in Firestore")
                        updateUI(user, "User", "Email", "$")
                    }
                    isAuthInProgress = false
                    loginButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    if (!isAuthInProgress) return@addOnFailureListener
                    

                    Log.e(TAG, "Error fetching user data", e)
                    Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateUI(user, "User", "Email", "$")
                    isAuthInProgress = false
                    loginButton.isEnabled = true
                }
        } else {
            updateUI(null, null, null, null)
            loginButton.isEnabled = true
        }
    }

    private fun updateUI(user: FirebaseUser?, username: String?, email: String?, preferredCurrency: String?) {
        if (user != null) {
            val welcomeMessage = if (!username.isNullOrEmpty()) "Welcome, $username" else "Welcome"
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ActivityHome::class.java)
            intent.putExtra("USERNAME", username ?: "User")
            intent.putExtra("EMAIL", email ?: "User")
            intent.putExtra("USER_ID", user.uid)
            intent.putExtra("CURRENCY", preferredCurrency ?: "$")
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        auth.currentUser?.reload()?.addOnCompleteListener {
            fetchUsernameAndUpdateUI(auth.currentUser)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}
