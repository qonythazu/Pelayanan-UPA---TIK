package com.dicoding.pelayananupa_tik.fcm

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.NotificationAdapter
import com.dicoding.pelayananupa_tik.backend.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class NotificationFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var currentUserEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        initializeFirebase()
        setupToolbar()
        setupRecyclerView()

        // Get current user email
        currentUserEmail = getCurrentUserEmail()

        if (currentUserEmail != null) {
            loadNotifications()
        } else {
            showEmptyState()
            Toast.makeText(context, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        rvNotifications = view.findViewById(R.id.rv_notifications)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter()
        rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email ?: run {
            // Fallback: coba ambil dari SharedPreferences jika ada
            val sharedPref = activity?.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPref?.getString("userEmail", null)
        }
    }

    private fun loadNotifications() {
        currentUserEmail?.let { email ->
            // Gunakan coroutine untuk operasi async
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val userNotifications = getUserNotifications(email)

                    if (userNotifications.isNotEmpty()) {
                        showNotifications(userNotifications)
                    } else {
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    Log.e("NotificationFragment", "Error loading notifications", e)
                    showEmptyState()
                    Toast.makeText(context, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun getUserNotifications(userEmail: String): List<NotificationModel> {
        return withContext(Dispatchers.IO) {
            val userNotifications = mutableListOf<NotificationModel>()

            try {
                Log.d("NotificationFragment", "Loading notifications for user: $userEmail")

                // Ambil semua notifikasi
                val notificationsSnapshot = firestore.collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                Log.d("NotificationFragment", "Found ${notificationsSnapshot.documents.size} total notifications")

                for (notificationDoc in notificationsSnapshot.documents) {
                    try {
                        // Parse manual untuk menghindari error timestamp
                        val data = notificationDoc.data
                        if (data != null) {
                            val documentId = data["documentId"] as? String
                            val collectionName = data["collectionName"] as? String
                            val title = data["title"] as? String
                            val body = data["body"] as? String

                            // Handle timestamp - bisa Timestamp atau String
                            val timestampString = when (val timestampValue = data["timestamp"]) {
                                is com.google.firebase.Timestamp -> timestampValue.toDate().toString()
                                is String -> timestampValue
                                else -> ""
                            }

                            Log.d("NotificationFragment", "Processing notification - Collection: $collectionName, DocumentId: $documentId")

                            if (!documentId.isNullOrEmpty() && !collectionName.isNullOrEmpty()) {
                                // Cek apakah form ini milik user yang sedang login
                                val isUserNotification = checkIfNotificationForUser(
                                    collectionName,
                                    documentId,
                                    userEmail
                                )

                                Log.d("NotificationFragment", "Is notification for user: $isUserNotification")

                                if (isUserNotification) {
                                    // Buat NotificationModel sesuai dengan struktur yang ada
                                    val notification = NotificationModel(
                                        title = title ?: "",
                                        body = body ?: "",
                                        timestamp = timestampString,
                                        documentId = notificationDoc.id, // Set dengan document ID dari Firestore
                                        collectionName = collectionName
                                    )
                                    userNotifications.add(notification)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationFragment", "Error parsing notification document", e)
                        // Lanjutkan ke document berikutnya
                    }
                }

                Log.d("NotificationFragment", "Final user notifications count: ${userNotifications.size}")

            } catch (e: Exception) {
                Log.e("NotificationFragment", "Error in getUserNotifications", e)
                throw e
            }

            userNotifications
        }
    }

    private suspend fun checkIfNotificationForUser(
        collectionName: String,
        documentId: String,
        userEmail: String
    ): Boolean {
        return try {
            Log.d("NotificationFragment", "Checking form in collection: $collectionName, documentId: $documentId")

            val formDoc = firestore.collection(collectionName)
                .document(documentId)
                .get()
                .await()

            if (formDoc.exists()) {
                val formUserEmail = formDoc.getString("userEmail")
                Log.d("NotificationFragment", "Form userEmail: $formUserEmail, Current user: $userEmail")
                formUserEmail == userEmail
            } else {
                Log.d("NotificationFragment", "Form document not found")
                false
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error checking form document", e)
            false
        }
    }

    private fun showNotifications(notifications: List<NotificationModel>) {
        rvNotifications.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        notificationAdapter.submitList(notifications)
    }

    private fun showEmptyState() {
        rvNotifications.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNavigation()
        (activity as? MainActivity)?.showToolbar()
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotificationFragment()
    }
}