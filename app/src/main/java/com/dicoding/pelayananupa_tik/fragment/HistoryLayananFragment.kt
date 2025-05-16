package com.dicoding.pelayananupa_tik.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ServiceHistoryPageAdapter
import com.google.android.material.tabs.TabLayout

class HistoryLayananFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: ServiceHistoryPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_layanan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager2 = view.findViewById(R.id.view_pager_2)

        adapter = ServiceHistoryPageAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager2.adapter = adapter

        val tabTitles = listOf("All", "Terkirim", "In-Review", "Diterima", "Proses Pengerjaan", "Ditolak", "Selesai")
        for (title in tabTitles) {
            val tab = tabLayout.newTab()
            val tabView = layoutInflater.inflate(R.layout.item_tab, tabLayout, false) as TextView
            tabView.text = title
            tab.customView = tabView
            tabLayout.addTab(tab)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                viewPager2.currentItem = position
                val tabTextView = tab.customView as? TextView

                when (tabTextView?.text) {
                    "Selesai" -> {
                        tabTextView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                    }
                    "Ditolak" -> {
                        tabTextView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    else -> {
                        tabTextView?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                    }
                }
                tabTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val tabTextView = tab?.customView as? TextView
                tabTextView?.setBackgroundColor(Color.WHITE)
                when (tabTextView?.text) {
                    "Selesai" -> {
                        tabTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    }
                    "Ditolak" -> {
                        tabTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    else -> {
                        tabTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
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