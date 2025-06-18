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
import com.google.firebase.firestore.QuerySnapshot

class DraftServiceFragment : Fragment() {

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

    interface OnDataChangedListener {
        fun onDataChanged()
    }

    private var onDataChangedListener: OnDataChangedListener? = null

    fun setOnDataChangedListener(listener: OnDataChangedListener) {
        this.onDataChangedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_draft_service, container, false)
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
            onEditItem = { layananItem, _ ->
                handleEditItem(layananItem)
            },
            onStatusChanged = { updatedItem, position ->
                handleStatusChange(updatedItem, position)
            },
            onDeleteItem = { layananItem, _ ->
                handleDeleteItem(layananItem)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun handleStatusChange(updatedItem: LayananItem, position: Int) {
        updateStatusToFirestore(updatedItem) { success ->
            if (success) {
                layananList.removeAt(position)
                adapter.notifyItemRemoved(position)
                showToast("Data berhasil dikirim")
                updateUI()
                onDataChangedListener?.onDataChanged()
                refreshTerkirimTab()
            } else {
                adapter.resetSubmitButton(position)
                showToast("Gagal mengirim data")
            }
        }
    }

    private fun handleDeleteItem(layananItem: LayananItem) {
        deleteFromFirestore(layananItem) { success ->
            if (success) {
                if (layananList.remove(layananItem)) {
                    adapter.notifyDataSetChanged()
                }
                showToast("Data berhasil dihapus")
                updateUI()
            } else {
                showToast("Gagal menghapus data")
            }
        }
    }

    private fun handleEditItem(layananItem: LayananItem) {
        Log.d("DraftService", "Edit draft item: ${layananItem.judul}")
        // TODO: Implement edit functionality
    }

    private fun fetchAllLayanan() {
        layananList.clear()
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

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "draft")
                .get()
                .addOnSuccessListener { documents ->
                    processDocuments(documents, collection)
                    completedCollections++
                    if (completedCollections == collections.size) {
                        updateUI()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents from $collection", e)
                    completedCollections++
                    if (completedCollections == collections.size) {
                        updateUI()
                    }
                }
        }
    }

    private fun processDocuments(documents: QuerySnapshot, collection: String) {
        for (doc in documents) {
            val layananItem = createLayananItem(doc, collection)
            layananList.add(layananItem)
        }
    }

    private fun createLayananItem(doc: QueryDocumentSnapshot, collection: String): LayananItem {
        val formType = getFormType(collection)

        return LayananItem(
            documentId = doc.id,
            judul = doc.getString("judul") ?: "Tidak ada judul",
            tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal",
            status = doc.getString("status") ?: "draft",
            formType = formType,

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

    private fun getFormType(collection: String): String {
        return when (collection) {
            "form_pemeliharaan" -> "pemeliharaan"
            "form_bantuan" -> "bantuan"
            "form_pemasangan" -> "pemasangan"
            "form_pembuatan" -> "pembuatan"
            "form_pengaduan" -> "pengaduan"
            "form_lapor_kerusakan" -> "lapor_kerusakan"
            else -> throw IllegalArgumentException("Unknown collection: $collection")
        }
    }

    private fun updateUI() {
        adapter.notifyDataSetChanged()
        if (layananList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
            emptyStateTextView.text = getString(R.string.belum_ada_pengajuan_layanan_di_draft)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
        }
    }

    private fun updateStatusToFirestore(updatedItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            callback(false)
            return
        }

        updateDocumentStatus(userEmail, updatedItem, callback)
    }

    private fun updateDocumentStatus(userEmail: String, updatedItem: LayananItem, callback: (Boolean) -> Unit) {
        var updateSuccess = false
        var completedCollections = 0

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("judul", updatedItem.judul)
                .whereEqualTo("timestamp", updatedItem.tanggal)
                .whereEqualTo("status", "draft")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() > 0 && !updateSuccess) {
                        val doc = documents.documents[0]
                        firestore.collection(collection)
                            .document(doc.id)
                            .update("status", "terkrim")
                            .addOnSuccessListener {
                                Log.d("DraftService", "Status updated successfully")
                                updateSuccess = true
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.w("DraftService", "Error updating status", e)
                                if (!updateSuccess) callback(false)
                            }
                    } else {
                        completedCollections++
                        if (completedCollections == collections.size && !updateSuccess) {
                            callback(false)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DraftService", "Error finding document", e)
                    completedCollections++
                    if (completedCollections == collections.size && !updateSuccess) {
                        callback(false)
                    }
                }
        }
    }

    private fun deleteFromFirestore(layananItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            callback(false)
            return
        }

        deleteDocumentFromFirestore(userEmail, layananItem, callback)
    }

    private fun deleteDocumentFromFirestore(userEmail: String, layananItem: LayananItem, callback: (Boolean) -> Unit) {
        var deleteSuccess = false
        var completedCollections = 0

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("judul", layananItem.judul)
                .whereEqualTo("timestamp", layananItem.tanggal)
                .whereEqualTo("status", "Draft")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() > 0 && !deleteSuccess) {
                        val doc = documents.documents[0]
                        firestore.collection(collection)
                            .document(doc.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("DraftService", "Document deleted successfully")
                                deleteSuccess = true
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.w("DraftService", "Error deleting document", e)
                                if (!deleteSuccess) callback(false)
                            }
                    } else {
                        completedCollections++
                        if (completedCollections == collections.size && !deleteSuccess) {
                            callback(false)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DraftService", "Error finding document", e)
                    completedCollections++
                    if (completedCollections == collections.size && !deleteSuccess) {
                        callback(false)
                    }
                }
        }
    }

    private fun refreshTerkirimTab() {
        Log.d("DraftService", "Refreshing Terkirim tab")
        val parentFragment = parentFragment as? HistoryLayananFragment
        if (parentFragment != null) {
            val fragmentManager = parentFragment.childFragmentManager
            val sentFragment = fragmentManager.findFragmentByTag("f1") as? SentServiceFragment
            sentFragment?.refreshData()

            Log.d("DraftService", "Found SentServiceFragment: ${sentFragment != null}")
        } else {
            Log.w("DraftService", "Parent HistoryLayananFragment not found")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}