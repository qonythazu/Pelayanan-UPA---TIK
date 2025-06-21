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
import com.google.firebase.firestore.QueryDocumentSnapshot

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
        setupViews(view)
        setupAdapter()
        fetchAllLayanan()
        return view
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupAdapter() {
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
                        refreshData() // Refresh seluruh data setelah delete
                    } else {
                        Toast.makeText(requireContext(), "Gagal membatalkan layanan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        recyclerView.adapter = adapter
    }

    private fun fetchAllLayanan() {
        layananList.clear() // Clear list sebelum fetch
        val userEmail = UserManager.getCurrentUserEmail()

        if (userEmail.isNullOrEmpty()) {
            Log.w("ServiceHistory", "User email is null or empty")
            updateUI()
            return
        }

        fetchDataFromCollections(userEmail)
    }

    private fun fetchDataFromCollections(userEmail: String) {
        var completedCollections = 0
        val tempList = mutableListOf<LayananItem>() // Temporary list untuk menghindari duplikasi

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "terkirim")
                .get()
                .addOnSuccessListener { documents ->
                    // Process documents dari collection ini
                    processDocuments(documents, collection, tempList)

                    completedCollections++
                    if (completedCollections == collections.size) {
                        // Semua collection sudah diproses, update UI
                        layananList.clear()
                        layananList.addAll(tempList)

                        // Sort berdasarkan timestamp (terbaru dulu)
                        layananList.sortByDescending { it.tanggal }

                        updateUI()
                        Log.d("SentService", "Total layanan terkirim: ${layananList.size}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents from $collection", e)
                    completedCollections++
                    if (completedCollections == collections.size) {
                        layananList.clear()
                        layananList.addAll(tempList)
                        layananList.sortByDescending { it.tanggal }
                        updateUI()
                    }
                }
        }
    }

    private fun processDocuments(documents: com.google.firebase.firestore.QuerySnapshot, collection: String, tempList: MutableList<LayananItem>) {
        for (doc in documents) {
            val layananItem = createLayananItem(doc, collection)

            // Check duplikasi berdasarkan documentId dan formType
            val isDuplicate = tempList.any {
                it.documentId == layananItem.documentId && it.formType == layananItem.formType
            }

            if (!isDuplicate) {
                tempList.add(layananItem)
                Log.d("SentService", "Added: ${layananItem.judul} from $collection")
            } else {
                Log.w("SentService", "Duplicate found for: ${layananItem.judul}")
            }
        }
    }

    private fun createLayananItem(doc: QueryDocumentSnapshot, collection: String): LayananItem {
        val formType = getFormTypeFromCollection(collection)

        return LayananItem(
            documentId = doc.id,
            judul = doc.getString("judul") ?: "Tidak ada judul",
            tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal",
            status = doc.getString("status") ?: "terkirim",
            formType = formType,

            // Fields sesuai dengan DraftServiceFragment
            kontak = when (formType) {
                "bantuan", "pemasangan", "pengaduan", "pembuatan", "lapor_kerusakan" -> doc.getString("kontak") ?: ""
                else -> ""
            },

            layanan = when (formType) {
                "pemeliharaan", "pengaduan", "pembuatan" -> doc.getString("layanan") ?: ""
                else -> ""
            },

            jenis = when (formType) {
                "pemeliharaan", "pemasangan" -> doc.getString("jenis") ?: ""
                else -> ""
            },

            akun = when (formType) {
                "pemeliharaan" -> doc.getString("akun") ?: ""
                else -> ""
            },

            alasan = when (formType) {
                "pemeliharaan" -> doc.getString("alasan") ?: ""
                else -> ""
            },

            keluhan = when (formType) {
                "pengaduan" -> doc.getString("keluhan") ?: ""
                else -> ""
            },

            filePath = when (formType) {
                "pemeliharaan", "bantuan", "pengaduan" -> doc.getString("filePath") ?: ""
                else -> ""
            },

            jumlah = when (formType) {
                "bantuan" -> doc.getString("jumlah") ?: ""
                else -> ""
            },

            tujuan = when (formType) {
                "bantuan", "pemasangan", "pembuatan" -> doc.getString("tujuan") ?: ""
                else -> ""
            },

            namaPerangkat = when (formType) {
                "lapor_kerusakan" -> doc.getString("namaPerangkat") ?: ""
                else -> ""
            },

            keterangan = when (formType) {
                "lapor_kerusakan" -> doc.getString("keterangan") ?: ""
                else -> ""
            },

            imagePath = when (formType) {
                "lapor_kerusakan" -> doc.getString("imagePath") ?: ""
                else -> ""
            },

            namaLayanan = when (formType) {
                "pembuatan" -> doc.getString("namaLayanan") ?: ""
                else -> ""
            }
        )
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
                // Remove dari local list juga
                layananList.remove(layananItem)
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
        Log.d("SentService", "Refreshing sent service data...")
        fetchAllLayanan()
    }
}