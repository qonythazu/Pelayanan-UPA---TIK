package com.dicoding.pelayananupa_tik.fragment.peminjaman

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.ProductAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class ItemHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: ProductAdapter
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
        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        historyAdapter = ProductAdapter(emptyList(), null, false)

        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        getHistoryFromFirestore()
    }

    private fun getHistoryFromFirestore() {
        val currentUserEmail = UserManager.getCurrentUserEmail()

        if (currentUserEmail.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("HistoryFragment", "Memulai ambil data riwayat untuk user: $currentUserEmail")
        db.collection("daftar_barang")
            .whereEqualTo("peminjam", currentUserEmail)
            .get()
            .addOnSuccessListener { result ->
                val historyList = mutableListOf<Barang>()

                for (document in result) {
                    val barang = document.toObject(Barang::class.java)
                    historyList.add(barang)
                    Log.d("HistoryFragment", "Data riwayat: ${barang.namaBarang} - Status: ${barang.status}")
                }

                Log.d("HistoryFragment", "Total riwayat untuk $currentUserEmail: ${historyList.size}")
                historyAdapter.updateList(historyList)

                if (historyList.isEmpty()) {
                    Toast.makeText(requireContext(), "Belum ada riwayat peminjaman", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("HistoryFragment", "Gagal mengambil data riwayat: ${e.message}", e)
                Toast.makeText(requireContext(), "Gagal mengambil data riwayat", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadHistoryData()
    }
}