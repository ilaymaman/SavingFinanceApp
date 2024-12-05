    package com.example.savingfinance

    import android.annotation.SuppressLint
    import android.content.Intent
    import android.os.Bundle
    import android.util.Log
    import android.view.Gravity
    import android.view.MenuItem
    import android.view.View
    import android.widget.Button
    import androidx.activity.ComponentActivity
    import androidx.core.view.GravityCompat
    import androidx.drawerlayout.widget.DrawerLayout
    import com.google.android.material.navigation.NavigationView

    class MainActivity : ComponentActivity() {

        private lateinit var drawerLayout: DrawerLayout
        private lateinit var navigationView: NavigationView
        private lateinit var openDrawerButton: Button

        @SuppressLint("MissingInflatedId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            // Initialize DrawerLayout and NavigationView
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.right_nav_view)
            openDrawerButton = findViewById(R.id.open_drawer_button)

            openDrawerButton.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.END) // Open the right-side navigation drawer
            }

            // Handle NavigationView item clicks
            navigationView.setNavigationItemSelectedListener { menuItem ->
                handleNavigationItemClick(menuItem)
                true
            }
        }

        // Handle navigation drawer item clicks
        private fun handleNavigationItemClick(menuItem: MenuItem) {
            when (menuItem.itemId) {
                R.id.profile -> {
                    val intent = Intent(this, ActivityProfile::class.java)
                    startActivity(intent)
                }
                R.id.settings -> {
                    val intent = Intent(this, ActivitySettings::class.java)
                    startActivity(intent)
                }
                R.id.logout -> {
                    // Perform logout action
                }
            }

            // Close the drawer after handling the item click
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // Existing methods
        fun openSignupActivity(view: View) {
            val intent = Intent(this, ActivitySignup::class.java)
            startActivity(intent)
        }

        fun openLoginActivity(view: View) {
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }

        fun openHomeActivity(view: View) {
            val intent = Intent(this, ActivityHome::class.java)
            startActivity(intent)
        }
    }
