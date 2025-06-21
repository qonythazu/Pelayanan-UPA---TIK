package com.dicoding.pelayananupa_tik.fragment.historyPeminjaman

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.PeminjamanAdapter
import com.dicoding.pelayananupa_tik.backend.model.FormPeminjaman
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class RejectedItemFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: PeminjamanAdapter
    private lateinit var tvEmptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rejected_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = PeminjamanAdapter(mutableListOf()) // Ubah dari emptyList() ke mutableListOf()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = historyAdapter
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
            .whereEqualTo("statusPeminjaman", "ditolak")
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
        }.toMutableList() // Konversi ke MutableList

        if (historyList.isEmpty()) {
            showEmptyState("Belum ada peminjaman yang ditolak")
        } else {
            showData(historyList)
        }
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility = View.GONE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = message
    }

    private fun showData(historyList: MutableList<Pair<String, FormPeminjaman>>) { // Ubah parameter ke MutableList
        recyclerView.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        historyAdapter.updateList(historyList)
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }
}