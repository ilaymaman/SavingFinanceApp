    package com.example.savingfinance

    import android.app.Activity
    import android.os.Bundle
    import android.util.Log
    import android.widget.Button
    import android.widget.EditText
    import android.widget.Toast
    import androidx.activity.ComponentActivity
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.FirebaseUser
    import com.google.firebase.auth.auth
    import com.google.firebase.Firebase

    class ActivitySignup : ComponentActivity() {

        // [START declare_auth]
        private lateinit var auth: FirebaseAuth
        // [END declare_auth]

        public override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_signup)
            // [START initialize_auth]
            // Initialize Firebase Auth
            auth = Firebase.auth

            // Handle UI actions (e.g., button clicks)
            val emailInput = findViewById<EditText>(R.id.nameEditText)
            val passwordInput = findViewById<EditText>(R.id.passwordEditText)
            val signUpButton = findViewById<Button>(R.id.signUpButton)

            signUpButton.setOnClickListener {
                val email = emailInput.text.toString()
                val password = passwordInput.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    createAccount(email, password)  // Call createAccount here
                } else {
                    Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // [START on_start_check_user]
        public override fun onStart() {
            super.onStart()
            // Check if user is signed in (non-null) and update UI accordingly.
            val currentUser = auth.currentUser
            if (currentUser != null) {
                reload()
            }
        }
        // [END on_start_check_user]

        private fun createAccount(email: String, password: String) {
            // [START create_user_with_email]
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null)
                    }
                }
            // [END create_user_with_email]
        }

        private fun signIn(email: String, password: String) {
            // [START sign_in_with_email]
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null)
                    }
                }
            // [END sign_in_with_email]
        }

        private fun sendEmailVerification() {
            // [START send_email_verification]
            val user = auth.currentUser!!
            user.sendEmailVerification()
                .addOnCompleteListener(this) { task ->
                    // Email Verification sent
                }
            // [END send_email_verification]
        }

        private fun updateUI(user: FirebaseUser?) {
            if (user != null) {
                Toast.makeText(this, "Welcome, ${user.email}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            }
        }

        private fun reload() {
            auth.currentUser?.reload()?.addOnCompleteListener {
                updateUI(auth.currentUser)
            }
        }

        companion object {
            private const val TAG = "EmailPassword"
        }
    }
