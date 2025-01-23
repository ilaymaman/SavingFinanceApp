package com.example.savingfinance

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.savingfinance.ActivitySignup.Companion.TAG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLogin : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            //reload()
        }

        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signUpText)

        loginButton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.emailEditText)
            val passwordInput = findViewById<EditText>(R.id.passwordEditText)
            val email = emailInput.text.toString()
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
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in success, fetch Firestore username
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    fetchUsernameAndUpdateUI(user)
                } else {
                    // If sign-in fails, display a message to the user
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null, null)
                }
            }
        // [END sign_in_with_email]
    }

    private fun fetchUsernameAndUpdateUI(user: FirebaseUser?) {
        if (user != null) {
            val userId = user.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "User"
                        updateUI(user, username)
                    } else {
                        Log.w(TAG, "No such document in Firestore")
                        updateUI(user, "User") // Fallback username
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user data", e)
                    updateUI(user, "User") // Fallback username
                }
        } else {
            updateUI(null, null)
        }
    }

    private fun updateUI(user: FirebaseUser?, username: String?) {
        if (user != null) {
            val welcomeMessage = if (!username.isNullOrEmpty()) "Welcome, $username" else "Welcome"
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ActivityHome::class.java)
            intent.putExtra("USERNAME", username ?: "User")
            startActivity(intent)
            finish() // Prevent going back to the login screen
        } else {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        auth.currentUser?.reload()?.addOnCompleteListener {
            fetchUsernameAndUpdateUI(auth.currentUser)
        }
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}
