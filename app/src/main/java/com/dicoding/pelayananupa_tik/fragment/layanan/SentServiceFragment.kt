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

        // Updated adapter initialization dengan callback functions
        adapter = LayananAdapter(
            layananList = layananList,
            onEditItem = { layananItem, position ->
                // Handle edit - untuk sent service mungkin tidak bisa diedit
                handleEditItem(layananItem, position)
            },
            onStatusChanged = { updatedItem, position ->
                // Untuk sent service, mungkin tidak ada perubahan status
                // atau bisa digunakan untuk fungsi lain seperti "batalkan pengiriman"
                handleStatusChange(updatedItem, position)
            },
            onDeleteItem = { layananItem, position ->
                // 1. Delete dari Firestore DULU (jika diizinkan)
                deleteFromFirestore(layananItem) { success ->
                    if (success) {
                        // 2. BARU hapus dari UI
                        layananList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()

                        // 3. Update UI untuk empty state
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
                .whereEqualTo("status", "Terkirim")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val judul = doc.getString("judul") ?: "Tidak ada judul"
                        val tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal"
                        val status = doc.getString("status") ?: "Tidak ada status"
                        val documentId = doc.id

                        // Tambahkan info untuk operasi Firestore
                        val layananItem = LayananItem(
                            judul = judul,
                            tanggal = tanggal,
                            status = status
                        ).apply {
                            // Simpan info untuk operasi CRUD
                            // Asumsikan LayananItem punya property ini
                            // this.documentId = documentId
                            // this.collectionName = collection
                        }

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

    // Fungsi untuk handle edit item (untuk sent service biasanya read-only)
    private fun handleEditItem(layananItem: LayananItem, position: Int) {
        // Sent service biasanya tidak bisa diedit, tapi bisa dibuka untuk view detail
        Log.d("SentService", "View sent item: ${layananItem.judul}")

        // Contoh: Buka detail view atau tampilkan pesan
        Toast.makeText(requireContext(), "Layanan yang sudah terkirim tidak dapat diedit", Toast.LENGTH_SHORT).show()

        // Atau buka detail view:
        // val intent = Intent(requireContext(), DetailLayananActivity::class.java)
        // intent.putExtra("layanan_item", layananItem)
        // intent.putExtra("is_read_only", true)
        // startActivity(intent)
    }

    // Fungsi untuk handle perubahan status (jika diperlukan)
    private fun handleStatusChange(updatedItem: LayananItem, position: Int) {
        // Untuk sent service, mungkin bisa digunakan untuk "batalkan" jika masih memungkinkan
        Log.d("SentService", "Status change requested for: ${updatedItem.judul}")

        // Contoh: Tampilkan dialog konfirmasi untuk batalkan
        // showCancelConfirmationDialog(updatedItem, position)

        // Atau tampilkan pesan bahwa tidak bisa diubah
        Toast.makeText(requireContext(), "Status layanan yang sudah terkirim tidak dapat diubah", Toast.LENGTH_SHORT).show()
    }

    // Fungsi untuk delete dari Firestore (jika diizinkan)
    private fun deleteFromFirestore(layananItem: LayananItem, callback: (Boolean) -> Unit) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (userEmail.isNullOrEmpty()) {
            callback(false)
            return
        }

        // Untuk sent service, mungkin tidak diizinkan dihapus
        // Atau bisa dihapus dengan konfirmasi khusus
        Log.d("SentService", "Delete request for sent item: ${layananItem.judul}")

        // Opsi 1: Tidak izinkan hapus
        Toast.makeText(requireContext(), "Layanan yang sudah terkirim tidak dapat dihapus", Toast.LENGTH_SHORT).show()
        callback(false)
        return

        // Opsi 2: Izinkan hapus dengan implementasi seperti ini:
        /*
        var deleteSuccess = false
        var completedCollections = 0

        for (collection in collections) {
            firestore.collection(collection)
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("judul", layananItem.judul)
                .whereEqualTo("timestamp", layananItem.tanggal)
                .whereEqualTo("status", "Terkirim")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() > 0 && !deleteSuccess) {
                        val doc = documents.documents[0]
                        firestore.collection(collection)
                            .document(doc.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("SentService", "Document deleted successfully")
                                deleteSuccess = true
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.w("SentService", "Error deleting document", e)
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
                    Log.w("SentService", "Error finding document", e)
                    completedCollections++
                    if (completedCollections == collections.size && !deleteSuccess) {
                        callback(false)
                    }
                }
        }
        */
    }

    // Fungsi public untuk refresh dari fragment lain
    fun refreshData() {
        fetchAllLayanan()
    }
}