package com.dicoding.pelayananupa_tik.addon

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FeedbackHelper(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private var feedbackListeners = mutableListOf<ListenerRegistration>()

    fun startFeedbackMonitoring() {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) return
        monitorPeminjaman(userEmail)
        val layananCollections = listOf(
            "form_bantuan", "form_pemasangan", "form_pembuatan",
            "form_pemeliharaan", "form_pengaduan", "form_lapor_kerusakan"
        )
        layananCollections.forEach { collection ->
            monitorLayanan(collection, userEmail)
        }
    }

    private fun monitorPeminjaman(userEmail: String) {
        val listener = db.collection("form_peminjaman")
            .whereEqualTo("userEmail", userEmail)
            .whereEqualTo("statusPeminjaman", "Selesai")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                snapshots?.documents?.forEach { document ->
                    checkAndShowFeedbackForPeminjaman(document)
                }
            }
        feedbackListeners.add(listener)
    }

    private fun monitorLayanan(collection: String, userEmail: String) {
        val listener = db.collection(collection)
            .whereEqualTo("userEmail", userEmail)
            .whereEqualTo("status", "Selesai")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                snapshots?.documents?.forEach { document ->
                    checkAndShowFeedbackForLayanan(document, collection)
                }
            }
        feedbackListeners.add(listener)
    }

    private fun checkAndShowFeedbackForPeminjaman(document: DocumentSnapshot) {
        db.collection("feedback")
            .whereEqualTo("documentId", document.id)
            .whereEqualTo("type", "peminjaman")
            .get()
            .addOnSuccessListener { feedbackSnapshots ->
                if (feedbackSnapshots.isEmpty) {
                    val userEmail = document.getString("userEmail") ?: ""
                    val namaPerangkat = document.getString("namaPerangkat") ?: ""

                    showFeedbackDialog(
                        documentId = document.id,
                        type = "peminjaman",
                        collection = "form_peminjaman",
                        message = "Halo $userEmail, status peminjaman $namaPerangkat kini telah selesai! Mohon berikan feedback"
                    )
                }
            }
    }

    private fun checkAndShowFeedbackForLayanan(document: DocumentSnapshot, collection: String) {
        db.collection("feedback")
            .whereEqualTo("documentId", document.id)
            .whereEqualTo("type", "layanan")
            .get()
            .addOnSuccessListener { feedbackSnapshots ->
                if (feedbackSnapshots.isEmpty) {
                    val userEmail = document.getString("userEmail") ?: ""
                    val judul = document.getString("judul") ?: ""

                    showFeedbackDialog(
                        documentId = document.id,
                        type = "layanan",
                        collection = collection,
                        message = "Halo $userEmail, status layanan $judul yang kamu ajukan kini telah selesai! Mohon berikan feedback"
                    )
                }
            }
    }

    private fun showFeedbackDialog(
        documentId: String,
        type: String,
        collection: String,
        message: String
    ) {
        if (context !is Activity) return

        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val etComment = dialogView.findViewById<EditText>(R.id.et_comment)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btn_submit)
        val btnSkip = dialogView.findViewById<Button>(R.id.btn_skip)

        tvMessage.text = message

        val dialog = builder.setView(dialogView)
            .setCancelable(false)
            .create()

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comment = etComment.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(context, "Mohon berikan rating!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitFeedback(documentId, type, collection, rating, comment) {
                dialog.dismiss()
                Toast.makeText(context, "Feedback berhasil dikirim!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSkip.setOnClickListener {
            submitFeedback(documentId, type, collection, 0f, "") {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun submitFeedback(
        documentId: String,
        type: String,
        collection: String,
        rating: Float,
        comment: String,
        onSuccess: () -> Unit
    ) {
        val userEmail = UserManager.getCurrentUserEmail() ?: ""

        val feedbackData = hashMapOf(
            "documentId" to documentId,
            "type" to type,
            "collection" to collection,
            "userEmail" to userEmail,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to FieldValue.serverTimestamp(),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal mengirim feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun stopFeedbackMonitoring() {
        feedbackListeners.forEach { it.remove() }
        feedbackListeners.clear()
    }
}