package com.dicoding.pelayananupa_tik.fragment.peminjamanFeature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.BoxAdapter
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel
import com.dicoding.pelayananupa_tik.databinding.FragmentBoxBinding

class BoxFragment : Fragment() {

    private var _binding: FragmentBoxBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BoxAdapter
    private lateinit var boxViewModel: BoxViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        boxViewModel = ViewModelProvider(requireActivity())[BoxViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = BoxAdapter(mutableListOf()) { barang, isChecked ->
            if (isChecked) {
                boxViewModel.selectItem(barang)
            } else {
                boxViewModel.unselectItem(barang)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                boxViewModel.selectAll()
                adapter.selectAll()
            } else {
                boxViewModel.unselectAll()
                adapter.unselectAll()
            }
        }

        binding.btnPinjam.setOnClickListener {
            proceedToPeminjaman()
        }
    }

    private fun observeData() {
        boxViewModel.boxItems.observe(viewLifecycleOwner) { items ->
            updateUI(items)
        }

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
        binding.recyclerView.visibility = View.GONE
        binding.checkboxSelectAll.visibility = View.GONE
        binding.btnPinjam.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
    }

    private fun showBoxContent(items: MutableList<Barang>) {
        binding.recyclerView.visibility = View.VISIBLE
        binding.checkboxSelectAll.visibility = View.VISIBLE
        binding.btnPinjam.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        adapter.updateList(items)
    }

    private fun updateButtonState(selectedItems: MutableList<Barang>) {
        val selectedCount = selectedItems.size
        if (selectedCount > 0) {
            binding.btnPinjam.text = getString(R.string.pinjam_with_count, selectedCount)
            binding.btnPinjam.isEnabled = true
        } else {
            binding.btnPinjam.text = getString(R.string.pinjam)
            binding.btnPinjam.isEnabled = false
        }
    }

    private fun updateSelectAllCheckbox(selectedItems: MutableList<Barang>) {
        val totalItems = boxViewModel.getBoxCount()
        val selectedCount = selectedItems.size

        binding.checkboxSelectAll.setOnCheckedChangeListener(null) // Remove listener temporarily

        when (selectedCount) {
            0 -> {
                binding.checkboxSelectAll.isChecked = false
                binding.checkboxSelectAll.text = getString(R.string.select_all)
            }
            totalItems -> {
                binding.checkboxSelectAll.isChecked = true
                binding.checkboxSelectAll.text = getString(R.string.select_all)
            }
            else -> {
                binding.checkboxSelectAll.isChecked = false
                binding.checkboxSelectAll.text = getString(R.string.select_all_with_count, selectedCount, totalItems)
            }
        }

        setupSelectAllListener()
    }

    private fun setupSelectAllListener() {
        binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
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
            val selectedItemNames = selectedItems.joinToString(", ") { it.namaBarang }
            val bundle = Bundle().apply {
                putString("selectedItems", selectedItemNames)
            }

            findNavController().navigate(
                R.id.action_boxFragment_to_formPeminjamanFragment,
                bundle
            )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}