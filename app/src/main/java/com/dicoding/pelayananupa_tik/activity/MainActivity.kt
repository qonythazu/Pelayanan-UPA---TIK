package com.dicoding.pelayananupa_tik.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.addon.FeedbackHelper
import com.dicoding.pelayananupa_tik.databinding.ActivityMainBinding
import com.dicoding.pelayananupa_tik.fcm.FCMManager
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.dicoding.pelayananupa_tik.utils.UserData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var fcmManager: FCMManager
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fcmManager.initializeFCM()
        }
    }

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

        feedbackHelper = FeedbackHelper(this)

        fcmManager = FCMManager(this)
        fcmManager.requestNotificationPermission(requestPermissionLauncher)
        fcmManager.initializeFCM()

        setupNavigation()
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

    private fun setupLogoutButton() {
        val btnLogout = findViewById<ImageView>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya") { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                val currentEmail = UserManager.getCurrentUserEmail()
                Log.d(TAG, "Logging out user: $currentEmail")
                UserManager.signOut()

                Toast.makeText(this@MainActivity, "Anda berhasil logout", Toast.LENGTH_SHORT).show()
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

        feedbackHelper.startFeedbackMonitoring()
        if (!UserManager.isUserLoggedIn()) {
            Log.w(TAG, "User session expired, redirecting to login")
            redirectToLogin()
        }
    }

    override fun onPause() {
        super.onPause()
        feedbackHelper.stopFeedbackMonitoring()
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