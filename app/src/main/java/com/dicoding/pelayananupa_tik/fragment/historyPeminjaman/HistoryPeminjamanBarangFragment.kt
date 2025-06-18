package com.dicoding.pelayananupa_tik.fragment.historyPeminjaman

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ItemHistoryPageAdapter
import com.dicoding.pelayananupa_tik.databinding.FragmentHistoryPeminjamanBarangBinding
import com.google.android.material.tabs.TabLayout

class HistoryPeminjamanBarangFragment : Fragment() {
    private var _binding: FragmentHistoryPeminjamanBarangBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ItemHistoryPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryPeminjamanBarangBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabLayout()
        setupViewPager()
        setupTabBehavior()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_historyPeminjamanBarangFragment_to_historyFragment)
            }
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.setSelectedTabIndicatorColor(Color.TRANSPARENT)

        val tabTitles = listOf("Diajukan", "Disetujui", "Diambil", "Ditolak", "Selesai")
        for (title in tabTitles) {
            val tab = binding.tabLayout.newTab()
            val tabView = layoutInflater.inflate(R.layout.item_tab, binding.tabLayout, false) as TextView
            tabView.text = title
            tabView.setBackgroundColor(Color.TRANSPARENT)
            tab.customView = tabView
            binding.tabLayout.addTab(tab)
        }
        setupInitialTabStyling()
    }

    private fun setupViewPager() {
        adapter = ItemHistoryPageAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager2.adapter = adapter
    }

    private fun setupTabBehavior() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                binding.viewPager2.currentItem = position
                val tabTextView = tab.customView as? TextView
                setSelectedTabStyle(tabTextView)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val tabTextView = tab?.customView as? TextView
                setUnselectedTabStyle(tabTextView)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselected if needed
            }
        })

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }

    private fun setupInitialTabStyling() {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            val tabTextView = tab?.customView as? TextView

            if (i == 0) {
                setSelectedTabStyle(tabTextView)
            } else {
                setUnselectedTabStyle(tabTextView)
            }
        }
    }

    private fun setUnselectedTabStyle(tabTextView: TextView?) {
        tabTextView?.let { textView ->
            val drawable = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(2, when (textView.text) {
                    "Selesai" -> ContextCompat.getColor(requireContext(), R.color.green)
                    "Ditolak" -> ContextCompat.getColor(requireContext(), R.color.red)
                    else -> ContextCompat.getColor(requireContext(), R.color.primary_blue)
                })
                cornerRadius = 24f // Rounded corners
            }

            textView.background = drawable
            textView.elevation = 4f // Shadow effect

            // Set text color
            when (textView.text) {
                "Selesai" -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                "Ditolak" -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                else -> textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            }
        }
    }

    private fun setSelectedTabStyle(tabTextView: TextView?) {
        tabTextView?.let { textView ->
            val drawable = GradientDrawable().apply {
                if (textView.text == "Selesai") {
                    setColor(ContextCompat.getColor(requireContext(), R.color.green))
                } else {
                    setColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                }
                cornerRadius = 24f
            }

            textView.background = drawable
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.elevation = 6f // Slightly higher elevation when selected
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