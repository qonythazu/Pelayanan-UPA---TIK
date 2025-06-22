package com.dicoding.pelayananupa_tik.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class FirestoreNotificationListener(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
                                    // Cek apakah notifikasi ini untuk user yang sedang login
                                    checkAndShowNotification(
                                        title = notification["title"] as? String ?: "Update",
                                        body = notification["body"] as? String ?: "Ada update baru",
                                        type = notification["type"] as? String ?: "",
                                        itemId = notification["documentId"] as? String ?: "",
                                        collectionName = notification["collectionName"] as? String ?: "",
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
        coroutineScope.cancel()
    }

    private fun checkAndShowNotification(
        title: String,
        body: String,
        type: String = "",
        itemId: String = "",
        collectionName: String = "",
        notificationDocId: String = ""
    ) {
        // Dapatkan email user yang sedang login
        val currentUserEmail = getCurrentUserEmail()

        if (currentUserEmail == null) {
            Log.w("NotificationListener", "User not authenticated")
            return
        }

        if (itemId.isNotEmpty() && collectionName.isNotEmpty()) {
            // Gunakan coroutine untuk pengecekan async
            coroutineScope.launch {
                try {
                    val isUserNotification = checkIfNotificationForUser(
                        collectionName,
                        itemId,
                        currentUserEmail
                    )

                    if (isUserNotification) {
                        // Tampilkan notifikasi hanya jika memang untuk user ini
                        showNotification(
                            title = title,
                            body = body,
                            type = type,
                            itemId = itemId,
                            notificationDocId = notificationDocId
                        )
                    }
                } catch (e: Exception) {
                    Log.e("NotificationListener", "Error checking notification ownership", e)
                }
            }
        }
    }

    private fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email ?: run {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPref?.getString("userEmail", null)
        }
    }

    private suspend fun checkIfNotificationForUser(
        collectionName: String,
        documentId: String,
        userEmail: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NotificationListener", "Checking form in collection: $collectionName, documentId: $documentId")

                val formDoc = firestore.collection(collectionName)
                    .document(documentId)
                    .get()
                    .await()

                if (formDoc.exists()) {
                    val formUserEmail = formDoc.getString("userEmail")
                    Log.d("NotificationListener", "Form userEmail: $formUserEmail, Current user: $userEmail")
                    formUserEmail == userEmail
                } else {
                    Log.d("NotificationListener", "Form document not found")
                    false
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error checking form document", e)
                false
            }
        }
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