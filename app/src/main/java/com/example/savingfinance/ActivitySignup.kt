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

    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog
    private lateinit var signUpButton: Button
    private val timeoutDuration = 15000L // 15 seconds timeout
    private val handler = Handler(Looper.getMainLooper())
    private var isAuthInProgress = false
    // [END declare_auth]

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        
        // Setup UI
        setContentView(R.layout.activity_signup)
        
        // Setup progress dialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Creating account...")
            setCancelable(false)
        }
        
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
        
        // Handle UI actions (e.g., button clicks)
        signUpButton = findViewById(R.id.signUpButton)
        val loginButton = findViewById<Button>(R.id.logInText)

        signUpButton.setOnClickListener {
            val usernameInput = findViewById<EditText>(R.id.nameEditText)
            val emailInput = findViewById<EditText>(R.id.emailEditText)
            val passwordInput = findViewById<EditText>(R.id.passwordEditText)
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                createAccount(email, password, username)  // Call createAccount here
            } else {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
            }
        }

        loginButton.setOnClickListener{
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }
    }

    // [END on_start_check_user]

    private fun createAccount(email: String, password: String, username: String) {
        if (isAuthInProgress) return
        
        isAuthInProgress = true
        progressDialog.show()
        signUpButton.isEnabled = false
        
        // Set timeout for signup
        val timeoutRunnable = Runnable {
            if (isAuthInProgress) {
                progressDialog.dismiss()
                Toast.makeText(this, "Registration timed out. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                isAuthInProgress = false
                signUpButton.isEnabled = true
            }
        }
        
        handler.postDelayed(timeoutRunnable, timeoutDuration)
        
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                handler.removeCallbacks(timeoutRunnable)
                
                if (!isAuthInProgress) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    // Save username to Firestore
                    user?.let {
                        val userId = it.uid
                        val userData = mapOf(
                            "username" to username,
                            "email" to email
                        )
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User data saved successfully")

                                val transaction = mapOf(
                                    "amount" to 0.0,
                                    "type" to "Initial Balance",
                                    "timestamp" to System.currentTimeMillis()
                                )

                                //transactions document
                                firestore.collection("users").document(userId)
                                    .collection("transactions")
                                    .add(transaction)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Transactions collection created with a default entry")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error adding transaction", e)
                                    }

                                //goals document
                                val goal = mapOf(
                                    "currentAmount" to 0,
                                    "goalAmount" to 0,
                                    "category" to "None",
                                    "name" to "My First Goal",
                                    "isMainGoal" to true
                                )
                                
                                firestore.collection("users").document(userId)
                                    .collection("goals")
                                    .add(goal)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Goals collection created with a default entry")
                                        progressDialog.dismiss()
                                        updateUI(user, username, email) // Navigate to activity_home
                                        isAuthInProgress = false
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error adding goal", e)
                                        progressDialog.dismiss()
                                        updateUI(user, username, email) // Continue anyway
                                        isAuthInProgress = false
                                    }
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Log.w(TAG, "Error saving user data", e)
                                Toast.makeText(
                                    this, "Registration successful but failed to save user data. Some features may be limited.", 
                                    Toast.LENGTH_LONG
                                ).show()
                                updateUI(user, username, email) // Continue anyway
                                isAuthInProgress = false
                                signUpButton.isEnabled = true
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    progressDialog.dismiss()
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
        // [END create_user_with_email]
    }

    private fun sendEmailVerification() {
        // [START send_email_verification]
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                Toast.makeText(this, "Check your email!", Toast.LENGTH_SHORT).show()
            }
        // [END send_email_verification]
    }

    private fun updateUI(user: FirebaseUser?, username: String, email: String) {
        if (user != null) {
            Toast.makeText(this, "Welcome, $username", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ActivityHome::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("EMAIL", email)
            intent.putExtra("USER_ID", user.uid)
            startActivity(intent)
            finish() // Optional: Prevent going back to signup screen
        } else {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        progressDialog.show()
        val user = auth.currentUser
        if (user != null) {
            // Fetch the username from Firestore
            val userId = user.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    progressDialog.dismiss()
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "User"
                        val email = document.getString("email") ?: "User"
                        updateUI(user, username, email) // Pass the username to updateUI
                    } else {
                        Log.w(TAG, "No such document in Firestore")
                        updateUI(user, "User", "Email") // Fallback username
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Log.e(TAG, "Error fetching user data", e)
                    updateUI(user, "User", "Email") // Fallback username in case of failure
                }
        } else {
            progressDialog.dismiss()
            updateUI(null, "", "")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    companion object {
        const val TAG = "EmailPassword"
    }
}
