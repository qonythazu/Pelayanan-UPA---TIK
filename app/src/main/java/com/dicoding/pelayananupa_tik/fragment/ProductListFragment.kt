package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ProductAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ProductListFragment : Fragment(R.layout.fragment_product_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var searchView: SearchView
    private lateinit var boxViewModel: BoxViewModel
    private val db = FirebaseFirestore.getInstance()
    private var fullList = mutableListOf<Barang>()
    private var availableList = mutableListOf<Barang>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize shared ViewModel
        boxViewModel = ViewModelProvider(requireActivity())[BoxViewModel::class.java]

        setupToolbar(view)
        initViews(view)
        setupRecyclerView()
        getBarangDariFirestore()
        setupSearch()
        observeBoxCount()
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.fragment_toolbar)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_box -> {
                    findNavController().navigate(R.id.action_productListFragment_to_boxFragment)
                    true
                }
                else -> false
            }
        }

        updateToolbarTitle()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        searchView = view.findViewById(R.id.search_view)
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(availableList) { barang ->
            addToBox(barang)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    private fun getBarangDariFirestore() {
        Log.d("FirestoreFetch", "Memulai ambil data dari Firestore...")

        db.collection("daftar_barang")
            .get()
            .addOnSuccessListener { result ->
                fullList.clear()
                availableList.clear()

                for (document in result) {
                    val barang = document.toObject(Barang::class.java)
                    Log.d("FirestoreFetch", "Data ditemukan: ${barang.namaBarang}, Status: ${barang.status}")

                    fullList.add(barang)

                    // Hanya tambahkan barang yang tersedia ke availableList
                    if (barang.status == "tersedia" || barang.status.isEmpty()) {
                        availableList.add(barang)
                    }
                }

                Log.d("FirestoreFetch", "Total data: ${fullList.size}, Tersedia: ${availableList.size}")
                adapter.updateList(availableList)
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
                val filtered = availableList.filter {
                    it.namaBarang.contains(newText ?: "", ignoreCase = true)
                }
                adapter.updateList(filtered)
                return true
            }
        })
    }

    private fun addToBox(barang: Barang) {
        if (boxViewModel.isItemInBox(barang)) {
            Toast.makeText(requireContext(), "${barang.namaBarang} sudah ada di box", Toast.LENGTH_SHORT).show()
        } else {
            boxViewModel.addToBox(barang)
            Toast.makeText(requireContext(), "${barang.namaBarang} ditambahkan ke box", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeBoxCount() {
        boxViewModel.boxCount.observe(viewLifecycleOwner) { count ->
            updateToolbarTitle()
        }
    }

    private fun updateToolbarTitle() {
        val toolbar = view?.findViewById<Toolbar>(R.id.fragment_toolbar)
        val boxCount = boxViewModel.getBoxCount()
        if (boxCount > 0) {
            toolbar?.title = "Daftar Barang (Box: $boxCount)"
        } else {
            toolbar?.title = "Daftar Barang"
        }
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