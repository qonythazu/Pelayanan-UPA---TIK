package com.dicoding.pelayananupa_tik.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        var title: String
        var body: String
        var type = ""
        var itemId = ""
        var status: String

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            title = notification.title ?: "Update"
            body = notification.body ?: "Status berubah"
            showNotification(title, body, type, itemId)
            return
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            type = remoteMessage.data["type"] ?: ""
            status = remoteMessage.data["status"] ?: ""
            itemId = remoteMessage.data["id"] ?: remoteMessage.data["documentId"] ?: ""
            title = remoteMessage.data["title"] ?: "Update Status"
            body = remoteMessage.data["body"] ?: generateStatusMessage(type, status)

            // Check for duplicate notifications
            val notificationKey = "${type}_${itemId}_${status}_${System.currentTimeMillis() / 10000}" // 10 second window
            if (!isDuplicateNotification(notificationKey)) {
                showNotification(title, body, type, itemId)
                saveNotificationToFirestore(title, body, type, status, itemId)
                markNotificationShown(notificationKey)
            }
        } else {
            showNotification("Update", "Ada update baru", "", "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    private fun isDuplicateNotification(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    private fun markNotificationShown(key: String) {
        sharedPreferences.edit().putBoolean(key, true).apply()

        // Clean up old keys (keep only last 100)
        val allKeys = sharedPreferences.all.keys.toList()
        if (allKeys.size > 100) {
            val editor = sharedPreferences.edit()
            allKeys.take(allKeys.size - 100).forEach { oldKey ->
                editor.remove(oldKey)
            }
            editor.apply()
        }
    }

    private fun generateStatusMessage(type: String, status: String): String {
        val normalizedType = when (type.lowercase()) {
            "bantuan", "pemasangan", "pembuatan", "pemeliharaan", "pengaduan", "lapor_kerusakan" -> "layanan"
            else -> type.lowercase()
        }

        return when (normalizedType) {
            "peminjaman", "status_update" -> {
                when (status) {
                    "Diajukan" -> "Pengajuan peminjaman Anda telah diterima"
                    "Disetujui" -> "Peminjaman Anda disetujui! Silakan ambil barang"
                    "Diambil" -> "Barang telah diambil. Jangan lupa kembalikan tepat waktu"
                    "Ditolak" -> "Maaf, pengajuan peminjaman Anda ditolak"
                    "Selesai" -> "Peminjaman telah selesai. Terima kasih!"
                    else -> "Status peminjaman berubah menjadi $status"
                }
            }
            "layanan" -> {
                when (status) {
                    "Draft" -> "Draft pengajuan layanan tersimpan"
                    "Terkirim" -> "Pengajuan layanan Anda telah dikirim"
                    "In-Review" -> "Pengajuan layanan sedang direview"
                    "Diterima" -> "Pengajuan layanan Anda diterima!"
                    "Proses Pengerjaan" -> "Layanan Anda sedang dalam proses pengerjaan"
                    "Ditolak" -> "Maaf, pengajuan layanan Anda ditolak"
                    "Selesai" -> "Layanan telah selesai dikerjakan"
                    else -> "Status layanan berubah menjadi $status"
                }
            }
            else -> {
                if (status.isNotEmpty()) {
                    "Status berubah menjadi $status"
                } else {
                    "Ada update terbaru untuk Anda"
                }
            }
        }
    }

    private fun showNotification(title: String, body: String, type: String = "", itemId: String = "") {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Status Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for status updates"
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (type.isNotEmpty()) {
                putExtra("notification_type", type)
                putExtra("item_id", itemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = firestore.collection("users").document(currentUser.uid)

            userRef.update(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener {
                // Token updated successfully
            }.addOnFailureListener {
                // Create new document if update fails
                val userData = mapOf(
                    "fcmToken" to token,
                    "email" to currentUser.email,
                    "uid" to currentUser.uid,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }

    private fun saveNotificationToFirestore(title: String, body: String, type: String, status: String, itemId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val notificationData = mapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "status" to status,
                "documentId" to itemId,
                "userEmail" to currentUser.email,
                "userId" to currentUser.uid,
                "read" to false,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "collectionName" to getCollectionNameFromType(type)
            )

            firestore.collection("notifications").add(notificationData)
                .addOnSuccessListener {
                    // Notification saved successfully
                }
                .addOnFailureListener {
                    // Handle error if needed
                }
        }
    }

    private fun getCollectionNameFromType(type: String): String {
        return when (type.lowercase()) {
            "peminjaman", "status_update" -> "form_peminjaman"
            "bantuan" -> "form_bantuan"
            "pemasangan" -> "form_pemasangan"
            "pembuatan" -> "form_pembuatan"
            "pemeliharaan" -> "form_pemeliharaan"
            "pengaduan" -> "form_pengaduan"
            "lapor_kerusakan" -> "form_lapor_kerusakan"
            else -> "general"
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_updates"
    }
}