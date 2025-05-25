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
import com.dicoding.pelayananupa_tik.utils.UserData
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

        if (UserManager.isUserLoggedIn()) {
            lifecycleScope.launch {
                UserManager.initializeUserData { success: Boolean, userData: UserData? ->
                    if (success) {
                        Log.d(TAG, "User data ready for: ${userData?.email}")
                    } else {
                        Log.e(TAG, "Failed to initialize user data")
                        handleUserDataError()
                    }
                }
            }
        } else {
            redirectToLogin()
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
        lifecycleScope.launch {
            UserManager.getCurrentUserData { userData: UserData? ->
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
                        handleUserDataError()
                    }
                }
            }
        }
    }

    private fun handleUserDataError() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Gagal memuat data pengguna. Silakan login ulang.",
                Toast.LENGTH_LONG
            ).show()
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
                val currentEmail = UserManager.getCurrentUserEmail()
                Log.d(TAG, "Logging out user: $currentEmail")
                UserManager.signOut()

                Toast.makeText(this@MainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()
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

        if (!UserManager.isUserLoggedIn()) {
            Log.w(TAG, "User session expired, redirecting to login")
            redirectToLogin()
        }
    }

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