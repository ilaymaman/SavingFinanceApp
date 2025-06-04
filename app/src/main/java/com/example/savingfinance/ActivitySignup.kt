package com.example.savingfinance

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class ActivitySignup : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var signUpButton: Button
    private val timeoutDuration = 15000L
    private val handler = Handler(Looper.getMainLooper())
    private var isAuthInProgress = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_signup)

        signUpButton = findViewById(R.id.signUpButton)
        val loginButton = findViewById<Button>(R.id.logInText)

        signUpButton.setOnClickListener {
            val usernameInput = findViewById<EditText>(R.id.nameEditText)
            val emailInput = findViewById<EditText>(R.id.emailEditText)
            val passwordInput = findViewById<EditText>(R.id.passwordEditText)
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val preferredCurrency = "$"

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                createAccount(email, password, username, preferredCurrency)
            } else {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
            }
        }

        loginButton.setOnClickListener{
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }
    }



    private fun createAccount(email: String, password: String, username: String, preferredCurrency: String) {
        if (isAuthInProgress) return
        
        isAuthInProgress = true
        signUpButton.isEnabled = false
        val timeoutRunnable = Runnable {
            if (isAuthInProgress) {
                Toast.makeText(this, "Registration timed out. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                isAuthInProgress = false
                signUpButton.isEnabled = true
            }
        }
        
        handler.postDelayed(timeoutRunnable, timeoutDuration)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                handler.removeCallbacks(timeoutRunnable)
                
                if (!isAuthInProgress) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    user?.let {
                        val userId = it.uid
                        val userData = mapOf(
                            "username" to username,
                            "email" to email,
                            "preferredCurrency" to "$"
                        )
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User data saved successfully")

                                Log.d(TAG, "Creating empty transactions and goals collections")

                                updateUI(user, username, email, preferredCurrency)
                                isAuthInProgress = false
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error saving user data", e)
                                Toast.makeText(
                                    this, "Registration successful but failed to save user data. Some features may be limited.", 
                                    Toast.LENGTH_LONG
                                ).show()
                                updateUI(user, username, email, preferredCurrency)
                                isAuthInProgress = false
                                signUpButton.isEnabled = true
                            }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    
                    val errorMessage = when(task.exception) {
                        is FirebaseAuthUserCollisionException -> "This email is already registered. Please login instead."
                        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters with letters and numbers."
                        else -> "Registration failed. ${task.exception?.message ?: "Please try again."}"
                    }
                    
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                    isAuthInProgress = false
                    signUpButton.isEnabled = true
                }
            }
    }

    private fun updateUI(user: FirebaseUser?, username: String, email: String, preferredCurrency: String) {
        if (user != null) {
            Toast.makeText(this, "Welcome, $username", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ActivityHome::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("EMAIL", email)
            intent.putExtra("USER_ID", user.uid)
            intent.putExtra("CURRENCY", preferredCurrency)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val TAG = "EmailPassword"
    }
}
