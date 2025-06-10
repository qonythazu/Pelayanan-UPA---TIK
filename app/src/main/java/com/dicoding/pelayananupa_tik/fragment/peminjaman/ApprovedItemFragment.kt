package com.dicoding.pelayananupa_tik.fragment.peminjaman

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.PeminjamanAdapter
import com.dicoding.pelayananupa_tik.backend.model.FormPeminjaman
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApprovedItemFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: PeminjamanAdapter
    private lateinit var tvEmptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_approved_item, container, false)
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
            emptyList(),
            onTakenClick = { documentId, formPeminjaman ->
                handleTakenClick(documentId, formPeminjaman)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = historyAdapter
        }
    }

    private fun handleTakenClick(documentId: String, formPeminjaman: FormPeminjaman) {
        // Konfirmasi dialog sebelum update status
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin sudah mengambil barang ini?")
            .setPositiveButton("Ya") { _, _ ->
                updateStatusToTaken(documentId)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun updateStatusToTaken(documentId: String) {
        // Tampilkan loading atau progress indicator jika diperlukan

        db.collection("form_peminjaman")
            .document(documentId)
            .update(
                mapOf(
                    "statusPeminjaman" to "diambil",
                    "tanggalPengambilan" to getCurrentDateTime() // Optional: tambah timestamp pengambilan
                )
            )
            .addOnSuccessListener {
                // Tampilkan pesan sukses
                Toast.makeText(context, "Status berhasil diupdate ke 'Diambil'", Toast.LENGTH_SHORT).show()

                // Refresh data
                loadHistoryData()
            }
            .addOnFailureListener { e ->
                // Tampilkan pesan error
                Toast.makeText(context, "Gagal mengupdate status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadHistoryData() {
        val currentUserEmail = UserManager.getCurrentUserEmail()

        if (currentUserEmail.isNullOrEmpty()) {
            showEmptyState("User tidak terautentikasi")
            return
        }

        db.collection("form_peminjaman")
            .whereEqualTo("userEmail", currentUserEmail)
            .whereEqualTo("statusPeminjaman", "disetujui")
            .get()
            .addOnSuccessListener { result ->
                processResults(result)
            }
            .addOnFailureListener { e ->
                showEmptyState("Gagal mengambil data: ${e.message}")
            }
    }

    private fun processResults(result: QuerySnapshot) {
        val historyList = result.mapNotNull { document ->
            try {
                val formPeminjaman = document.toObject(FormPeminjaman::class.java)
                Pair(document.id, formPeminjaman) // Pair<DocumentId, FormPeminjaman>
            } catch (e: Exception) {
                null
            }
        }

        if (historyList.isEmpty()) {
            showEmptyState("Belum ada peminjaman yang disetujui")
        } else {
            showData(historyList)
        }
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility = View.GONE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = message
    }

    private fun showData(historyList: List<Pair<String, FormPeminjaman>>) {
        recyclerView.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        historyAdapter.updateList(historyList)
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }
}