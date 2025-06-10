package com.dicoding.pelayananupa_tik.fragment.layanan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.adapter.LayananAdapter
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class AcceptedServiceFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_accepted_service, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateTextView = view.findViewById(R .id.emptyStateTextView)

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
                .whereEqualTo("status", "diterima")
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
            emptyStateTextView.text = getString(R.string.belum_ada_layanan_yang_diterima)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
        }
    }
}