    package com.example.savingfinance

    import android.app.Activity
    import android.content.Intent
    import android.os.Bundle
    import android.util.Log
    import android.widget.Button
    import android.widget.EditText
    import android.widget.Toast
    import androidx.activity.ComponentActivity
    import com.google.firebase.Firebase
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.FirebaseUser
    import com.google.firebase.auth.auth
    import com.google.firebase.firestore.FirebaseFirestore

    class ActivitySignup : ComponentActivity() {

        // [START declare_auth]
        private lateinit var auth: FirebaseAuth
        private lateinit var firestore: FirebaseFirestore
        // [END declare_auth]

        public override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // [START initialize_auth]
            // Initialize Firebase Auth
            auth = Firebase.auth
            firestore = FirebaseFirestore.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                //reload()
            }
            setContentView(R.layout.activity_signup)
            // Handle UI actions (e.g., button clicks)

            val signUpButton = findViewById<Button>(R.id.signUpButton)
            val loginButton = findViewById<Button>(R.id.logInText)

            signUpButton.setOnClickListener {
                val usernameInput = findViewById<EditText>(R.id.nameEditText)
                val emailInput = findViewById<EditText>(R.id.emailEditText)
                val passwordInput = findViewById<EditText>(R.id.passwordEditText)
                val username = usernameInput.text.toString()
                val email = emailInput.text.toString()
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
            // [START create_user_with_email]
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
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

                                    val goal = mapOf(
                                        "currentAmount" to 0,
                                        "GoalAmount" to 100,
                                        "Catagory" to "Gift",
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
                                    firestore.collection("users").document(userId)
                                        .collection("goals")
                                        .add(goal)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Goals collection created with a default entry")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "Error adding goal", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error saving user data", e)
                                    Toast.makeText(
                                        this, "Failed to save user data", Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        updateUI(user, username, email) // Navigate to activity_home
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null, username, email)
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
            val user = auth.currentUser
            if (user != null) {
                // Fetch the username from Firestore
                val userId = user.uid
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
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
                        Log.e(TAG, "Error fetching user data", e)
                        updateUI(user, "User", "Email") // Fallback username in case of failure
                    }
            } else {
                updateUI(null, "", "")
            }
        }

        companion object {
            const val TAG = "EmailPassword"
        }
    }
