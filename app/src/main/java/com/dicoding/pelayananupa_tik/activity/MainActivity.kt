package com.dicoding.pelayananupa_tik.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.ActivityMainBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestCodeNotificationPermission = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        FirebaseFirestore.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is properly logged in and initialized
        if (!UserManager.isUserLoggedIn()) {
            Log.w(TAG, "User not logged in, redirecting to login")
            redirectToLogin()
            return
        }

        setupNavigation()
        setupPermissions()
        setupLogoutButton()
        loadUserInfo()
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.bottomNav
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_home_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navView.setupWithNavController(navController)
    }

    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    requestCodeNotificationPermission
                )
            }
        }
    }

    private fun setupLogoutButton() {
        val btnLogout = findViewById<ImageView>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserInfo() {
        // Load current user info for logging purposes
        lifecycleScope.launch {
            UserManager.getCurrentUserData { userData ->
                runOnUiThread {
                    if (userData != null) {
                        Log.d(TAG, "MainActivity loaded for user: ${userData.email}")

                        if (UserManager.isCurrentUserPredefined()) {
                            Log.d(TAG, "Predefined user: ${userData.namaLengkap} (${userData.nim})")
                        } else {
                            Log.d(TAG, "Generic user: ${userData.email}")
                        }
                    } else {
                        Log.w(TAG, "Failed to load user data")
                        // Could redirect to login if user data is critical
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeNotificationPermission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin notifikasi diperlukan untuk melanjutkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Get current user info for logging
                val currentEmail = UserManager.getCurrentUserEmail()
                Log.d(TAG, "Logging out user: $currentEmail")

                // Sign out using UserManager (which handles Firebase Auth)
                UserManager.signOut()

                Toast.makeText(this@MainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()

                // Redirect to login
                redirectToLogin()

            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                Toast.makeText(this@MainActivity, "Terjadi kesalahan saat logout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        // Check if user is still logged in when activity resumes
        if (!UserManager.isUserLoggedIn()) {
            Log.w(TAG, "User session expired, redirecting to login")
            redirectToLogin()
        }
    }

    // Methods for fragment management
    fun hideBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
    }

    fun showBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.VISIBLE
    }

    fun hideToolbar() {
        findViewById<Toolbar>(R.id.app_bar).visibility = View.GONE
    }

    fun showToolbar() {
        findViewById<Toolbar>(R.id.app_bar).visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}