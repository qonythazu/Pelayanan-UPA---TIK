package com.dicoding.pelayananupa_tik.fragment.layanan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.LayananAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ServiceHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LayananAdapter
    private val layananList = mutableListOf<String>()
    private val firestore = FirebaseFirestore.getInstance()
    private val collections = listOf(
        "form_bantuan_operator",
        "form_pemasangan",
        "form_pembuatan_web_dll",
        "form_pemeliharaan_akun",
        "form_pengaduan"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_service_history, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        adapter = LayananAdapter(layananList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchAllLayanan()
        return view
    }

    private fun fetchAllLayanan() {
        layananList.clear()

        var counter = 0
        for (collection in collections) {
            firestore.collection(collection).get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val layanan = doc.getString("layanan")
                        layanan?.let { layananList.add(it) }
                    }
                    counter++
                    if (counter == collections.size) {
                        adapter.notifyItemRangeInserted(0, layananList.size)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents from $collection", e)
                    counter++
                }
        }
    }
}