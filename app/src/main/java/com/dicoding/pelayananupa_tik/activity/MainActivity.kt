package com.dicoding.pelayananupa_tik.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import com.dicoding.pelayananupa_tik.fcm.FirestoreNotificationListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var feedbackHelper: FeedbackHelper
    private lateinit var fcmManager: FCMManager
    private lateinit var notificationListener: FirestoreNotificationListener
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fcmManager.initializeFCM()
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fcmManager.initializeFCM()
        } else {
            Log.w("FCM", "Notification permission denied")
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

                        // Start notification listener setelah user data ready
                        startNotificationListener()
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
        requestNotificationPermissionAndInitialize()
        handleNotificationIntent(intent)
        debugFCMToken()

        setupNavigation()
        setupLogoutButton()
        loadUserInfo()
    }

    private fun startNotificationListener() {
        notificationListener = FirestoreNotificationListener(this)
        notificationListener.startListening()
        Log.d(TAG, "Notification listener started")
    }

    private fun debugFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_DEBUG", "Current FCM Token: $token")
                Log.d("FCM_DEBUG", "User UID: ${FirebaseAuth.getInstance().currentUser?.uid}")
                Log.d("FCM_DEBUG", "User Email: ${FirebaseAuth.getInstance().currentUser?.email}")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun requestNotificationPermissionAndInitialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission sudah granted
                    fcmManager.initializeFCM()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale dialog
                    showNotificationPermissionRationale()
                }
                else -> {
                    // Request permission
                    fcmManager.requestNotificationPermission(notificationPermissionLauncher)
                }
            }
        } else {
            // Android < 13, langsung initialize
            fcmManager.initializeFCM()
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Izin Notifikasi Diperlukan")
            .setMessage("Aplikasi memerlukan izin notifikasi untuk memberitahu Anda tentang update status peminjaman dan layanan.")
            .setPositiveButton("Izinkan") { _, _ ->
                fcmManager.requestNotificationPermission(notificationPermissionLauncher)
            }
            .setNegativeButton("Tidak Sekarang") { _, _ ->
                Log.w("FCM", "User declined notification permission")
            }
            .show()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val notificationType = it.getStringExtra("notification_type")
            val itemId = it.getStringExtra("item_id")

            if (!notificationType.isNullOrEmpty()) {
                Log.d("FCM", "Handling notification click: $notificationType, itemId: $itemId")

                // Navigate ke fragment/activity yang sesuai
                when (notificationType) {
                    "peminjaman" -> {
                        // Navigate ke peminjaman detail atau list
                        // navigateToPeminjamanDetail(itemId)
                    }
                    "layanan" -> {
                        // Navigate ke layanan detail atau list
                        // navigateToLayananDetail(itemId)
                    }
                }
            }
        }
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

                // Stop notification listener sebelum logout
                if (::notificationListener.isInitialized) {
                    notificationListener.stopListening()
                }

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
        } else {
            // Restart listener jika user masih login
            if (::notificationListener.isInitialized) {
                notificationListener.startListening()
            } else {
                startNotificationListener()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        feedbackHelper.stopFeedbackMonitoring()

        // Stop listener ketika app di background untuk hemat battery
        if (::notificationListener.isInitialized) {
            notificationListener.stopListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop listener
        if (::notificationListener.isInitialized) {
            notificationListener.stopListening()
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