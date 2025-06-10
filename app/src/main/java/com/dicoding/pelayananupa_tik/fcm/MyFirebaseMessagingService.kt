package com.dicoding.pelayananupa_tik.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"] ?: ""
            val status = remoteMessage.data["status"] ?: ""
            val itemId = remoteMessage.data["id"] ?: ""

            handleDataMessage(type, status, itemId, remoteMessage)
        }

        // Handle notification payload (jika ada)
        remoteMessage.notification?.let {
            showNotification(it.title ?: "Update", it.body ?: "Status berubah")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token ke Firestore
        saveTokenToFirestore(token)
    }

    private fun handleDataMessage(type: String, status: String, itemId: String, remoteMessage: RemoteMessage) {
        val title = when (type) {
            "peminjaman" -> "Update Peminjaman"
            "layanan" -> "Update Layanan"
            else -> "Update Status"
        }

        val body = generateStatusMessage(type, status)
        showNotification(title, body)
    }

    private fun generateStatusMessage(type: String, status: String): String {
        return when (type) {
            "peminjaman" -> {
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
            else -> "Status berubah menjadi $status"
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "status_updates",
                "Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for status updates"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent untuk buka app ketika notification diklik
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, "status_updates")
            .setSmallIcon(R.drawable.img_bell) // Pastikan icon ada
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveTokenToFirestore(token: String) {
        // Implementasi save token ke user document
        // Contoh implementation di MainActivity atau service lain
    }
}