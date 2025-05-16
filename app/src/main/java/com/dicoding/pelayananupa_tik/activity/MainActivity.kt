package com.dicoding.pelayananupa_tik.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestCodeNotificationPermission = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        FirebaseFirestore.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.bottomNav
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_home_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    requestCodeNotificationPermission
                )
            }
        }

        // Listener untuk tombol logout
        val btnLogout = findViewById<ImageView>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            logout()
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
        FirebaseAuth.getInstance().signOut() // Logout dari Firebase
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        // Kembali ke LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Tutup MainActivity
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
}