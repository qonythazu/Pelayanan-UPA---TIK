package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ProductAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel
import com.dicoding.pelayananupa_tik.databinding.FragmentProductListBinding
import com.google.firebase.firestore.FirebaseFirestore

class ProductListFragment : Fragment() {
    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductAdapter
    private lateinit var boxViewModel: BoxViewModel
    private val db = FirebaseFirestore.getInstance()
    private var fullList = mutableListOf<Barang>()
    private var availableList = mutableListOf<Barang>()
    private var badgeTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        boxViewModel = ViewModelProvider(requireActivity())[BoxViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        getBarangDariFirestore()
        setupSearch()
        observeBoxCount()
        observeRefreshNeeded()
    }

    private fun observeRefreshNeeded() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_needed")
            ?.observe(viewLifecycleOwner) { refreshNeeded ->
                if (refreshNeeded == true) {
                    refreshBarangData()
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("refresh_needed", false)
                }
            }
    }

    private fun setupToolbar() {
        binding.fragmentToolbar.apply {
            navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.toolbar_menu)

            // Sesuaikan padding menu items
            post {
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    if (child is androidx.appcompat.widget.ActionMenuView) {
                        for (j in 0 until child.childCount) {
                            val menuItem = child.getChildAt(j)
                            menuItem.setPadding(0, 0, 40, 0)
                        }
                    }
                }
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_box -> {
                        findNavController().navigate(R.id.action_productListFragment_to_boxFragment)
                        true
                    }
                    else -> false
                }
            }
        }
        setupBadge()
    }

    private fun setupBadge() {
        val toolbar = binding.fragmentToolbar
        val menuItem = toolbar.menu.findItem(R.id.menu_box)

        if (menuItem != null) {
            // Inflate custom badge layout
            val badgeLayout = LayoutInflater.from(requireContext())
                .inflate(R.layout.badge_menu_item, toolbar, false)

            badgeTextView = badgeLayout.findViewById(R.id.badge_text)
            menuItem.actionView = badgeLayout

            badgeLayout.setOnClickListener {
                findNavController().navigate(R.id.action_productListFragment_to_boxFragment)
            }

            updateBadge()
        } else {
            Log.w("ProductListFragment", "Menu item R.id.menu_box not found")
        }
    }

    private fun updateBadge() {
        val boxCount = boxViewModel.getBoxCount()

        badgeTextView?.apply {
            if (boxCount > 0) {
                visibility = View.VISIBLE
                text = if (boxCount > 99) "99+" else boxCount.toString()
            } else {
                visibility = View.GONE
            }
        }

        Log.d("ProductListFragment", "Badge updated: $boxCount items")
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(availableList, { barang ->
            addToBox(barang)
        }, true)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProductListFragment.adapter
        }
    }

    private fun getBarangDariFirestore() {
        Log.d("FirestoreFetch", "Memulai ambil data dari Firestore...")

        db.collection("daftar_barang")
            .get()
            .addOnSuccessListener { result ->
                try {
                    fullList.clear()
                    availableList.clear()

                    for (document in result) {
                        try {
                            val barang = document.toObject(Barang::class.java)
                            Log.d("FirestoreFetch", "Data ditemukan: ${barang.namaBarang}, Status: ${barang.status}")

                            fullList.add(barang)

                            // Hanya tambahkan barang yang tersedia ke availableList
                            if (barang.status == "tersedia" || barang.status.isEmpty()) {
                                availableList.add(barang)
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreFetch", "Error parsing document ${document.id}: ${e.message}")
                        }
                    }

                    Log.d("FirestoreFetch", "Total data: ${fullList.size}, Tersedia: ${availableList.size}")
                    adapter.updateList(availableList)

                    // Clear search if there's an active query
                    if (!binding.searchView.query.isNullOrEmpty()) {
                        binding.searchView.setQuery("", false)
                        binding.searchView.clearFocus()
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreFetch", "Error processing Firestore data: ${e.message}")
                    showErrorToast("Error memproses data")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreFetch", "Gagal ambil data: ${e.message}", e)
                showErrorToast("Gagal mengambil data")
            }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim() ?: ""
                val filtered = if (query.isEmpty()) {
                    availableList
                } else {
                    availableList.filter {
                        it.namaBarang.contains(query, ignoreCase = true) || it.jenis.contains(query, ignoreCase = true)
                    }
                }
                adapter.updateList(filtered)
                return true
            }
        })

        binding.searchView.setOnClickListener {
            binding.searchView.isIconified = false
        }
    }

    private fun addToBox(barang: Barang) {
        try {
            if (boxViewModel.isItemInBox(barang)) {
                showInfoToast("${barang.namaBarang} sudah ada di box")
            } else {
                boxViewModel.addToBox(barang)
                showSuccessToast("${barang.namaBarang} ditambahkan ke box")
            }
        } catch (e: Exception) {
            Log.e("ProductListFragment", "Error adding item to box: ${e.message}")
            showErrorToast("Gagal menambahkan item ke box")
        }
    }

    private fun observeBoxCount() {
        boxViewModel.boxCount.observe(viewLifecycleOwner) { count ->
            updateBadge()
            Log.d("ProductListFragment", "Box count changed: $count")
        }
    }

    private fun refreshBarangData() {
        Log.d("ProductListFragment", "Refreshing barang data...")
        getBarangDariFirestore()
    }

    private fun showSuccessToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showInfoToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        try {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.hideBottomNavigation()
                mainActivity.hideToolbar()
            }
            refreshBarangData()
            updateBadge()
        } catch (e: Exception) {
            Log.e("ProductListFragment", "Error in onResume: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.showBottomNavigation()
                mainActivity.showToolbar()
            }
        } catch (e: Exception) {
            Log.e("ProductListFragment", "Error in onPause: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        badgeTextView = null
    }
}