package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.BoxAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel

class BoxFragment : Fragment(R.layout.fragment_box) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BoxAdapter
    private lateinit var boxViewModel: BoxViewModel
    private lateinit var checkboxSelectAll: CheckBox
    private lateinit var btnPinjam: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel - shared with ProductListFragment
        boxViewModel = ViewModelProvider(requireActivity())[BoxViewModel::class.java]

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        checkboxSelectAll = view.findViewById(R.id.checkbox_select_all)
        btnPinjam = view.findViewById(R.id.btn_pinjam)
    }

    private fun setupRecyclerView() {
        adapter = BoxAdapter(mutableListOf()) { barang, isChecked ->
            if (isChecked) {
                boxViewModel.selectItem(barang)
            } else {
                boxViewModel.unselectItem(barang)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        // Checkbox Select All
        checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                boxViewModel.selectAll()
                adapter.selectAll()
            } else {
                boxViewModel.unselectAll()
                adapter.unselectAll()
            }
        }

        // Button Pinjam
        btnPinjam.setOnClickListener {
            proceedToPeminjaman()
        }
    }

    private fun observeData() {
        // Observe box items
        boxViewModel.boxItems.observe(viewLifecycleOwner) { items ->
            updateUI(items)
        }

        // Observe selected items
        boxViewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            updateButtonState(selectedItems)
            updateSelectAllCheckbox(selectedItems)
            adapter.updateSelectedItems(selectedItems)
        }
    }

    private fun updateUI(items: MutableList<Barang>) {
        if (items.isEmpty()) {
            showEmptyState()
        } else {
            showBoxContent(items)
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        checkboxSelectAll.visibility = View.GONE
        btnPinjam.visibility = View.GONE

        // You can add empty state view if needed
        Toast.makeText(requireContext(), "Box kosong. Tambahkan barang dari daftar produk.", Toast.LENGTH_LONG).show()
    }

    private fun showBoxContent(items: MutableList<Barang>) {
        recyclerView.visibility = View.VISIBLE
        checkboxSelectAll.visibility = View.VISIBLE
        btnPinjam.visibility = View.VISIBLE

        adapter.updateList(items)
    }

    private fun updateButtonState(selectedItems: MutableList<Barang>) {
        val selectedCount = selectedItems.size
        if (selectedCount > 0) {
            btnPinjam.text = "Pinjam ($selectedCount)"
            btnPinjam.isEnabled = true
        } else {
            btnPinjam.text = "Pinjam"
            btnPinjam.isEnabled = false
        }
    }

    private fun updateSelectAllCheckbox(selectedItems: MutableList<Barang>) {
        val totalItems = boxViewModel.getBoxCount()
        val selectedCount = selectedItems.size

        checkboxSelectAll.setOnCheckedChangeListener(null) // Remove listener temporarily

        when {
            selectedCount == 0 -> {
                checkboxSelectAll.isChecked = false
                checkboxSelectAll.text = "Pilih Semua"
            }
            selectedCount == totalItems -> {
                checkboxSelectAll.isChecked = true
                checkboxSelectAll.text = "Pilih Semua"
            }
            else -> {
                checkboxSelectAll.isChecked = false
                checkboxSelectAll.text = "Pilih Semua ($selectedCount/$totalItems)"
            }
        }

        // Restore listener
        setupSelectAllListener()
    }

    private fun setupSelectAllListener() {
        checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                boxViewModel.selectAll()
                adapter.selectAll()
            } else {
                boxViewModel.unselectAll()
                adapter.unselectAll()
            }
        }
    }

    private fun proceedToPeminjaman() {
        val selectedItems = boxViewModel.getSelectedItems()
        val selectedCount = selectedItems.size

        if (selectedCount > 0) {
            // Navigate to FormPeminjaman with selected items
            // You can pass selected items via Safe Args or ViewModel
            Toast.makeText(requireContext(), "Lanjut ke Form Peminjaman dengan $selectedCount barang", Toast.LENGTH_SHORT).show()

            // Example navigation (uncomment when FormPeminjaman is ready):
            // findNavController().navigate(R.id.action_boxFragment_to_formPeminjamanFragment)
        } else {
            Toast.makeText(requireContext(), "Pilih minimal 1 barang untuk dipinjam", Toast.LENGTH_SHORT).show()
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