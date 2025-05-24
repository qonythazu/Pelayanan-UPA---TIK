package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ProductAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.google.firebase.firestore.FirebaseFirestore

class ProductListFragment : Fragment(R.layout.fragment_product_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var searchView: SearchView
    private val db = FirebaseFirestore.getInstance()
    private var fullList = mutableListOf<Barang>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.fragment_toolbar)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_box -> {
                    true
                }
                else -> false
            }
        }

        recyclerView = view.findViewById(R.id.recycler_view)
        searchView = view.findViewById(R.id.search_view)

        adapter = ProductAdapter(fullList) { barang ->
            Toast.makeText(requireContext(), "${barang.namaBarang} ditambahkan", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        getBarangDariFirestore()
        setupSearch()
    }

    private fun getBarangDariFirestore() {
        Log.d("FirestoreFetch", "Memulai ambil data dari Firestore...")

        db.collection("daftar_barang")
            .get()
            .addOnSuccessListener { result ->
                fullList.clear()
                for (document in result) {
                    val barang = document.toObject(Barang::class.java)
                    Log.d("FirestoreFetch", "Data ditemukan: ${barang.namaBarang}")
                    fullList.add(barang)
                }

                Log.d("FirestoreFetch", "Total data: ${fullList.size}")
                adapter.updateList(fullList)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreFetch", "Gagal ambil data: ${e.message}", e)
                Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = fullList.filter {
                    it.namaBarang.contains(newText ?: "", ignoreCase = true)
                }
                adapter.updateList(filtered)
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNavigation()
        (activity as? MainActivity)?.showToolbar()
    }
}