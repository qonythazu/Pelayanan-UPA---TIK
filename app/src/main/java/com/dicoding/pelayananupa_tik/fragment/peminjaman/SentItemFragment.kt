package com.dicoding.pelayananupa_tik.fragment.peminjaman

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = PeminjamanAdapter(emptyList())
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        val currentUserEmail = UserManager.getCurrentUserEmail()

        // Debug 1: Cek email user
        Log.d("SentItemFragment", "Current user email: $currentUserEmail")

        if (currentUserEmail.isNullOrEmpty()) {
            Log.w("SentItemFragment", "User email is null or empty")
            showEmptyState("User tidak terautentikasi")
            return
        }

        // Debug 2: Cek semua dokumen dulu tanpa filter
        db.collection("form_peminjaman")
            .get()
            .addOnSuccessListener { allResult ->
                Log.d("SentItemFragment", "Total documents in collection: ${allResult.size()}")

                // Debug 3: Print semua dokumen
                allResult.forEach { doc ->
                    Log.d("SentItemFragment", "Document ID: ${doc.id}")
                    Log.d("SentItemFragment", "Document data: ${doc.data}")

                    // Cek apakah ada field userEmail
                    val docUserEmail = doc.getString("userEmail")
                    Log.d("SentItemFragment", "Document userEmail: $docUserEmail")

                    val status = doc.getString("statusPeminjaman")
                    Log.d("SentItemFragment", "Document status: $status")
                }

                // Sekarang query dengan filter
                queryWithFilter(currentUserEmail)
            }
            .addOnFailureListener { e ->
                Log.e("SentItemFragment", "Error loading all data: ${e.message}")
            }
    }

    private fun queryWithFilter(currentUserEmail: String) {
        Log.d("SentItemFragment", "Querying with filter - email: $currentUserEmail")

        db.collection("form_peminjaman")
            .whereEqualTo("userEmail", currentUserEmail)
            .whereEqualTo("statusPeminjaman", "Diajukan")
            .get()
            .addOnSuccessListener { result ->
                Log.d("SentItemFragment", "Filtered query found ${result.size()} documents")

                if (result.isEmpty) {
                    Log.w("SentItemFragment", "No documents match the filter criteria")

                    // Test tanpa filter status
                    db.collection("form_peminjaman")
                        .whereEqualTo("userEmail", currentUserEmail)
                        .get()
                        .addOnSuccessListener { resultWithoutStatus ->
                            Log.d("SentItemFragment", "Without status filter: ${resultWithoutStatus.size()} documents")
                            processResults(resultWithoutStatus)
                        }
                } else {
                    processResults(result)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SentItemFragment", "Error loading filtered data: ${e.message}")
                showEmptyState("Gagal mengambil data: ${e.message}")
            }
    }

    private fun processResults(result: QuerySnapshot) {
        Log.d("SentItemFragment", "=== PROCESSING ${result.size()} DOCUMENTS ===")

        val historyList = result.mapNotNull { document ->
            try {
                Log.d("SentItemFragment", "Processing document: ${document.id}")

                // Debug raw data dulu
                val rawData = document.data
                Log.d("SentItemFragment", "Raw document data keys: ${rawData.keys}")

                // Cek field penting
                Log.d("SentItemFragment", "namaPerangkat: ${document.getString("namaPerangkat")}")
                Log.d("SentItemFragment", "statusPeminjaman: ${document.getString("statusPeminjaman")}")
                Log.d("SentItemFragment", "tanggalPengajuan: ${document.getString("tanggalPengajuan")}")
                Log.d("SentItemFragment", "barangDipinjam: ${document.get("barangDipinjam")}")

                val form = document.toObject(FormPeminjaman::class.java)

                // Debug parsed object
                Log.d("SentItemFragment", "=== PARSED OBJECT ===")
                Log.d("SentItemFragment", "namaPerangkat: '${form.namaPerangkat}'")
                Log.d("SentItemFragment", "statusPeminjaman: '${form.statusPeminjaman}'")
                Log.d("SentItemFragment", "tanggalPengajuan: '${form.tanggalPengajuan}'")
                Log.d("SentItemFragment", "barangDipinjam size: ${form.barangDipinjam.size}")

                if (form.barangDipinjam.isNotEmpty()) {
                    form.barangDipinjam.forEachIndexed { index, barang ->
                        Log.d("SentItemFragment", "barangDipinjam[$index]: nama='${barang.nama}', jenis='${barang.jenis}'")
                    }
                }

                // Test helper functions
                Log.d("SentItemFragment", "getNamaBarang(): '${form.getNamaBarang()}'")
                Log.d("SentItemFragment", "getJenisBarang(): '${form.getJenisBarang()}'")
                Log.d("SentItemFragment", "===================")

                form
            } catch (e: Exception) {
                Log.e("SentItemFragment", "Error parsing document ${document.id}: ${e.message}")
                Log.e("SentItemFragment", "Exception details: ", e)
                null
            }
        }

        Log.d("SentItemFragment", "Final history list size: ${historyList.size}")

        // Debug setiap item di list final
        historyList.forEachIndexed { index, item ->
            Log.d("SentItemFragment", "historyList[$index]: ${item.namaPerangkat} - ${item.statusPeminjaman}")
        }

        if (historyList.isEmpty()) {
            Log.w("SentItemFragment", "History list is empty after processing")
            showEmptyState("Belum ada peminjaman yang diajukan")
        } else {
            Log.d("SentItemFragment", "Calling showData with ${historyList.size} items")
            showData(historyList)
        }
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility = View.GONE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = message
    }

    private fun showData(historyList: List<FormPeminjaman>) {
        Log.d("SentItemFragment", "=== SHOW DATA CALLED ===")
        Log.d("SentItemFragment", "RecyclerView visibility before: ${recyclerView.visibility}")
        Log.d("SentItemFragment", "Empty message visibility before: ${tvEmptyMessage.visibility}")

        recyclerView.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE

        Log.d("SentItemFragment", "RecyclerView visibility after: ${recyclerView.visibility}")
        Log.d("SentItemFragment", "Empty message visibility after: ${tvEmptyMessage.visibility}")

        Log.d("SentItemFragment", "Calling adapter updateList with ${historyList.size} items")
        historyAdapter.updateList(historyList)

        Log.d("SentItemFragment", "Adapter item count: ${historyAdapter.itemCount}")
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }
}