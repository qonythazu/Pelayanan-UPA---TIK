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
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class ServiceHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LayananAdapter
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

        adapter = LayananAdapter(layananList)
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
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val judul = doc.getString("judul") ?: "Tidak ada judul"
                        val tanggal = doc.getString("timestamp") ?: "Tidak ada tanggal"
                        val status = doc.getString("status") ?: "Tidak ada status"

                        layananList.add(LayananItem(judul, tanggal, status))
                    }
                    counter++
                    if (counter == collections.size) {
                        adapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("FirestoreError", "Error getting documents from $collection", e)
                    counter++
                    if (counter == collections.size) {
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }
}