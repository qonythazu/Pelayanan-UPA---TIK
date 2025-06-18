package com.dicoding.pelayananupa_tik.fragment.historyLayanan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.LayananAdapter
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class SentServiceFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LayananAdapter
    private lateinit var emptyStateTextView: TextView
    private val layananList = mutableListOf<LayananItem>()
    private val firestore = FirebaseFirestore.getInstance()
    private val collections = listOf(
        "form_bantuan",
        "form_pemasangan",
        "form_pembuatan",
        "form_pemeliharaan",
        "form_pengaduan",
        "form_lapor_kerusakan"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sent_service, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        adapter = LayananAdapter(
            layananList = layananList,
            onEditItem = { layananItem, position ->
                handleEditItem(layananItem, position)
            },
            onStatusChanged = { updatedItem, position ->
                handleStatusChange(updatedItem, position)
            },
            onDeleteItem = { layananItem, _ ->
                deleteFromFirestore(layananItem) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Layanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                        updateUI()
                    } else {
                        Toast.makeText(requireContext(), "Gagal membatalkan layanan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchAllLayanan()
        return view
    }

    private fun fetchAllLayanan() {
        layananList.clear()

        val userEmail = UserManager.getCurrentUserEmail()

        if (userEmail.isNullOrEmpty()) {
            Log.w("ServiceHistory", "User email is null or empty")
            return
        }

        var counter = 0
        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "terkirim")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val judul = doc.getString("judul") ?: "Tidak ada judul"
                        val tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal"
                        val status = doc.getString("status") ?: "Tidak ada status"
                        val documentId = doc.id
                        val layananItem = LayananItem(
                            judul = judul,
                            tanggal = tanggal,
                            status = status,
                            documentId = documentId,
                            formType = getFormTypeFromCollection(collection)
                        )

                        layananList.add(layananItem)
                    }
                    counter++
                    if (counter == collections.size) {
                        updateUI()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents from $collection", e)
                    counter++
                    if (counter == collections.size) {
                        updateUI()
                    }
                }
        }
    }

    private fun getFormTypeFromCollection(collection: String): String {
        return when (collection) {
            "form_bantuan" -> "bantuan"
            "form_pemasangan" -> "pemasangan"
            "form_pembuatan" -> "pembuatan"
            "form_pemeliharaan" -> "pemeliharaan"
            "form_pengaduan" -> "pengaduan"
            "form_lapor_kerusakan" -> "lapor_kerusakan"
            else -> "unknown"
        }
    }

    private fun updateUI() {
        adapter.notifyDataSetChanged()
        if (layananList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
            emptyStateTextView.text = getString(R.string.belum_ada_layanan_yang_terkirim)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
        }
    }

    private fun handleEditItem(layananItem: LayananItem, position: Int) {
        Log.d("SentService", "View sent item: ${layananItem.judul}")
        Toast.makeText(requireContext(), "Layanan yang sudah terkirim tidak dapat diedit", Toast.LENGTH_SHORT).show()
    }

    private fun handleStatusChange(updatedItem: LayananItem, position: Int) {
        Log.d("SentService", "Status change requested for: ${updatedItem.judul}")
        Toast.makeText(requireContext(), "Status layanan yang sudah terkirim tidak dapat diubah", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFromFirestore(layananItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            Log.e("SentService", "User email is null or empty")
            callback(false)
            return
        }

        if (layananItem.documentId.isEmpty() || layananItem.formType.isEmpty()) {
            Log.e("SentService", "Document ID or form type is missing")
            callback(false)
            return
        }

        val collectionName = getCollectionFromFormType(layananItem.formType)
        if (collectionName == null) {
            Log.e("SentService", "Unknown form type: ${layananItem.formType}")
            callback(false)
            return
        }

        Log.d("SentService", "Deleting document: ${layananItem.documentId} from collection: $collectionName")
        firestore.collection(collectionName)
            .document(layananItem.documentId)
            .delete()
            .addOnSuccessListener {
                Log.d("SentService", "Document successfully deleted from Firestore")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SentService", "Error deleting document from Firestore", e)
                callback(false)
            }
    }

    private fun getCollectionFromFormType(formType: String): String? {
        return when (formType) {
            "bantuan" -> "form_bantuan"
            "pemasangan" -> "form_pemasangan"
            "pembuatan" -> "form_pembuatan"
            "pemeliharaan" -> "form_pemeliharaan"
            "pengaduan" -> "form_pengaduan"
            "lapor_kerusakan" -> "form_lapor_kerusakan"
            else -> null
        }
    }

    fun refreshData() {
        fetchAllLayanan()
    }
}