package com.dicoding.pelayananupa_tik.fragment.historyPeminjaman

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.PeminjamanAdapter
import com.dicoding.pelayananupa_tik.backend.model.FormPeminjaman
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SentItemFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: PeminjamanAdapter
    private lateinit var tvEmptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()
    private val historyList = mutableListOf<Pair<String, FormPeminjaman>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sent_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = PeminjamanAdapter(
            historyList = historyList,
            onTakenClick = null, // Tidak diperlukan untuk status "diajukan"
            onReturnedClick = null, // Tidak diperlukan untuk status "diajukan"
            onCancelClick = { documentId, formPeminjaman, position ->
                handleCancelPeminjaman(documentId, formPeminjaman, position)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = historyAdapter
        }
    }

    private fun handleCancelPeminjaman(documentId: String, formPeminjaman: FormPeminjaman, position: Int) {
        val currentUserEmail = UserManager.getCurrentUserEmail()

        if (currentUserEmail.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("SentItemFragment", "Canceling peminjaman: $documentId")

        // Hapus dari Firestore
        db.collection("form_peminjaman")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d("SentItemFragment", "Document successfully deleted from Firestore")

                // Update local list juga (sudah dilakukan di adapter, tapi pastikan konsisten)
                val itemToRemove = historyList.find { it.first == documentId }
                if (itemToRemove != null) {
                    val actualPosition = historyList.indexOf(itemToRemove)
                    if (actualPosition != -1 && actualPosition < historyList.size) {
                        // Item sudah dihapus dari adapter, tapi pastikan juga dihapus dari list lokal
                        // Tidak perlu hapus lagi karena sudah dilakukan di adapter
                        Log.d("SentItemFragment", "Local list updated, remaining items: ${historyList.size}")
                    }
                }

                // Update UI jika list kosong
                if (historyList.isEmpty()) {
                    showEmptyState("Belum ada peminjaman yang diajukan")
                }

                Toast.makeText(requireContext(), "Peminjaman berhasil dibatalkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("SentItemFragment", "Error deleting document from Firestore", e)

                // Kembalikan item ke list karena gagal hapus dari Firestore
                if (position < historyList.size) {
                    // Re-add item yang sudah dihapus dari adapter
                    val deletedItem = Pair(documentId, formPeminjaman)
                    historyList.add(position, deletedItem)
                    historyAdapter.notifyItemInserted(position)
                    historyAdapter.notifyItemRangeChanged(position, historyList.size)
                }

                Toast.makeText(
                    requireContext(),
                    "Gagal membatalkan peminjaman: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadHistoryData() {
        val currentUserEmail = UserManager.getCurrentUserEmail()

        if (currentUserEmail.isNullOrEmpty()) {
            showEmptyState("User tidak terautentikasi")
            return
        }

        db.collection("form_peminjaman")
            .whereEqualTo("userEmail", currentUserEmail)
            .whereEqualTo("statusPeminjaman", "diajukan")
            .get()
            .addOnSuccessListener { result ->
                processResults(result)
            }
            .addOnFailureListener { e ->
                Log.e("SentItemFragment", "Error fetching data", e)
                showEmptyState("Gagal mengambil data: ${e.message}")
            }
    }

    private fun processResults(result: QuerySnapshot) {
        historyList.clear() // Clear list sebelum populate

        val newHistoryList = result.mapNotNull { document ->
            try {
                val formPeminjaman = document.toObject(FormPeminjaman::class.java)
                Pair(document.id, formPeminjaman)
            } catch (e: Exception) {
                Log.e("SentItemFragment", "Error parsing document: ${document.id}", e)
                null
            }
        }

        historyList.addAll(newHistoryList)

        if (historyList.isEmpty()) {
            showEmptyState("Belum ada peminjaman yang diajukan")
        } else {
            showData()
        }
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility = View.GONE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = message
    }

    private fun showData() {
        recyclerView.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        historyAdapter.notifyDataSetChanged()
    }

    fun refreshData() {
        Log.d("SentItemFragment", "Refreshing sent item data...")
        loadHistoryData()
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }
}