package com.dicoding.pelayananupa_tik.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FirestoreNotificationListener(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        listenerRegistration = firestore.collection("notifications")
            .whereEqualTo("userEmail", "admin@example.com")
            .whereEqualTo("read", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->

                if (error != null || snapshots == null) {
                    return@addSnapshotListener
                }

                for (docChange in snapshots.documentChanges) {
                    when (docChange.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val notification = docChange.document.data

                            val timestamp = notification["timestamp"] as? com.google.firebase.Timestamp
                            if (timestamp != null) {
                                val now = System.currentTimeMillis()
                                val notificationTime = timestamp.toDate().time
                                val diffMinutes = (now - notificationTime) / (1000 * 60)

                                if (diffMinutes <= 1) {
                                    showNotification(
                                        title = notification["title"] as? String ?: "Update",
                                        body = notification["body"] as? String ?: "Ada update baru",
                                        type = notification["type"] as? String ?: "",
                                        itemId = notification["documentId"] as? String ?: "",
                                        notificationDocId = docChange.document.id
                                    )
                                }
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        }
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String = "",
        itemId: String = "",
        notificationDocId: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (type.isNotEmpty()) {
                putExtra("notification_type", type)
                putExtra("item_id", itemId)
                putExtra("notification_doc_id", notificationDocId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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

        markNotificationAsRead(notificationDocId)
    }

    private fun markNotificationAsRead(notificationDocId: String) {
        if (notificationDocId.isNotEmpty()) {
            firestore.collection("notifications")
                .document(notificationDocId)
                .update("read", true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_updates"
    }
}