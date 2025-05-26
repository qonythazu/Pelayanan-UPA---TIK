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
import androidx.recyclerview.widget.GridLayoutManager
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

    // Badge views
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

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_needed")?.observe(viewLifecycleOwner) { refreshNeeded ->
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

        updateToolbarTitle()
        setupBadge()
    }

    private fun setupBadge() {
        val toolbar = binding.fragmentToolbar

        // Cari menu item box
        val menuItem = toolbar.menu.findItem(R.id.menu_box)

        // Inflate custom badge layout
        val badgeLayout = LayoutInflater.from(requireContext())
            .inflate(R.layout.badge_menu_item, toolbar, false)

        badgeTextView = badgeLayout.findViewById(R.id.badge_text)
        menuItem.actionView = badgeLayout
        badgeLayout.setOnClickListener {
            findNavController().navigate(R.id.action_productListFragment_to_boxFragment)
        }

        updateBadge()
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
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@ProductListFragment.adapter
        }
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

                if (!binding.searchView.query.isNullOrEmpty()) {
                    binding.searchView.setQuery("", false)
                    binding.searchView.clearFocus()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreFetch", "Gagal ambil data: ${e.message}", e)
                Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
            updateBadge()
            Log.d("ProductListFragment", "Box count changed: $count")
        }
    }

    private fun updateToolbarTitle() {
        boxViewModel.getBoxCount()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
        refreshBarangData()
        updateBadge()
    }

    private fun refreshBarangData() {
        Log.d("ProductListFragment", "Refreshing barang data...")
        getBarangDariFirestore()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNavigation()
        (activity as? MainActivity)?.showToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}