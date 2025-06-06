package com.dicoding.pelayananupa_tik.fragment.layanan

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_service_history, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)

        // Updated adapter initialization dengan callback functions
        adapter = LayananAdapter(
            layananList = layananList,
            onEditItem = { layananItem, position ->
                // Handle edit - untuk draft biasanya bisa diedit
                handleEditItem(layananItem, position)
            },
            onStatusChanged = { updatedItem, position ->
                // 1. Update ke Firestore DULU (dari Draft ke Terkirim)
                updateStatusToFirestore(updatedItem) { success ->
                    if (success) {
                        // 2. BARU hapus dari Draft list dan refresh tabs
                        layananList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "Data berhasil dikirim", Toast.LENGTH_SHORT).show()

                        // 3. Update UI untuk empty state
                        updateUI()

                        // 4. Refresh tab Terkirim
                        refreshTerkirimTab()
                    } else {
                        // 5. Reset button jika gagal
                        adapter.resetSubmitButton(position)
                        Toast.makeText(requireContext(), "Gagal mengirim data", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeleteItem = { layananItem, _ ->
                deleteFromFirestore(layananItem) { success ->
                    if (success) {
                        // ✅ Remove berdasarkan object, bukan index
                        if (layananList.remove(layananItem)) {
                            adapter.notifyDataSetChanged() // Refresh seluruh adapter
                        }

                        Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                        updateUI()
                    } else {
                        Toast.makeText(requireContext(), "Gagal menghapus data", Toast.LENGTH_SHORT).show()
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
                .whereEqualTo("status", "Draft")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val judul = doc.getString("judul") ?: "Tidak ada judul"
                        val tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal"
                        val status = doc.getString("status") ?: "Tidak ada status"
                        val documentId = doc.id

                        // ✅ TAMBAHKAN INI - ambil field yang dibutuhkan untuk edit
                        val layanan = doc.getString("layanan") ?: ""
                        val jenis = doc.getString("jenis") ?: ""
                        val akun = doc.getString("akun") ?: ""
                        val alasan = doc.getString("alasan") ?: ""
                        val filePath = doc.getString("filePath") ?: ""

                        val layananItem = LayananItem(
                            documentId = documentId,  // ✅ TAMBAHKAN
                            judul = judul,
                            tanggal = tanggal,
                            status = status,
                            layanan = layanan,        // ✅ TAMBAHKAN
                            jenis = jenis,            // ✅ TAMBAHKAN
                            akun = akun,              // ✅ TAMBAHKAN
                            alasan = alasan,          // ✅ TAMBAHKAN
                            filePath = filePath,      // ✅ TAMBAHKAN
                            formType = getFormType(collection) // ✅ TAMBAHKAN
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

    // Helper function untuk mapping collection ke formType
    private fun getFormType(collection: String): String {
        return when (collection) {
            "form_pemeliharaan" -> "pemeliharaan_akun"
            "form_bantuan" -> "bantuan"
            "form_pemasangan" -> "pemasangan"
            // dst...
            else -> "pemeliharaan_akun" // default
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

    // Fungsi untuk handle edit item (khusus untuk draft)
    private fun handleEditItem(layananItem: LayananItem, position: Int) {
        // Draft bisa diedit, buka form edit
        Log.d("DraftService", "Edit draft item: ${layananItem.judul}")

        // Contoh: Buka activity edit atau dialog
        // val intent = Intent(requireContext(), EditLayananActivity::class.java)
        // intent.putExtra("layanan_item", layananItem)
        // intent.putExtra("is_draft", true)
        // startActivityForResult(intent, EDIT_REQUEST_CODE)
    }

    // Fungsi untuk update status dari Draft ke Terkirim
    private fun updateStatusToFirestore(updatedItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            callback(false)
            return
        }

        // Cari dan update document yang sesuai
        // Karena kita perlu mencari berdasarkan judul dan timestamp
        var updateSuccess = false
        var completedCollections = 0

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("judul", updatedItem.judul)
                .whereEqualTo("timestamp", updatedItem.tanggal)
                .whereEqualTo("status", "Draft")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() > 0 && !updateSuccess) {
                        // Update status menjadi "Terkirim"
                        val doc = documents.documents[0]
                        firestore.collection(collection)
                            .document(doc.id)
                            .update("status", "Terkirim")
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

    // Fungsi untuk delete dari Firestore
    private fun deleteFromFirestore(layananItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            callback(false)
            return
        }

        // Cari dan hapus document yang sesuai
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

    // Fungsi untuk refresh tab Terkirim
    private fun refreshTerkirimTab() {
        Log.d("DraftService", "Refreshing Terkirim tab")}
    fun refreshData() {
        fetchAllLayanan()
    }
}