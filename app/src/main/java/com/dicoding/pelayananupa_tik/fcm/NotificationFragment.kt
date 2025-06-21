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
            // SOLUSI 1: Query berdasarkan document ID yang mengandung user email
            // Jika document ID menggunakan format seperti: "userId_timestamp" atau "userEmail_timestamp"

            firestore.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val notifications = mutableListOf<NotificationModel>()

                        for (document in snapshot.documents) {
                            try {
                                // Check apakah document ID mengandung email user saat ini
                                // atau sesuai dengan pattern yang Anda gunakan
                                if (isNotificationForCurrentUser(document.id, email)) {
                                    val notification = document.toObject(NotificationModel::class.java)
                                    notification?.let {
                                        // Set documentId dari document ID
                                        val notificationWithId = it.copy(documentId = document.id)
                                        notifications.add(notificationWithId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("NotificationFragment", "Error parsing notification", e)
                            }
                        }

                        if (notifications.isNotEmpty()) {
                            showNotifications(notifications)
                        } else {
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("NotificationFragment", "Error loading notifications", exception)
                    showEmptyState()
                    Toast.makeText(context, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Function untuk mengecek apakah notification ini untuk user saat ini
     * berdasarkan document ID
     *
     * Sesuaikan logika ini dengan format document ID yang Anda gunakan
     */
    private fun isNotificationForCurrentUser(documentId: String, userEmail: String): Boolean {
        // Contoh implementasi berdasarkan berbagai format document ID:

        // Format 1: documentId mengandung email user
        if (documentId.contains(userEmail, ignoreCase = true)) {
            return true
        }

        // Format 2: documentId dimulai dengan email user
        if (documentId.startsWith(userEmail, ignoreCase = true)) {
            return true
        }
        return false
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