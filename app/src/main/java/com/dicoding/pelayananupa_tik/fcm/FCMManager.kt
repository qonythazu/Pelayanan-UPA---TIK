package com.dicoding.pelayananupa_tik.fcm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class FCMManager(private val context: android.content.Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun initializeFCM() {
        // Request permission untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Request permission di Activity
                return
            }
        }

        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")

            // Save token to Firestore
            saveTokenToUser(token)
        }
    }

    private fun saveTokenToUser(token: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = firestore.collection("users").document(currentUser.uid)

            userRef.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "FCM token saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Error saving FCM token", e)

                    // Jika document belum ada, create new
                    val userData = mapOf(
                        "fcmToken" to token,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
                }
        }
    }

    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}