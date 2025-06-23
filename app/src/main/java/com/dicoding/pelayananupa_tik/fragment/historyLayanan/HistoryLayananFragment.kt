package com.dicoding.pelayananupa_tik.fragment.historyLayanan

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.adapter.ServiceHistoryPageAdapter
import com.dicoding.pelayananupa_tik.databinding.FragmentHistoryLayananBinding
import com.google.android.material.tabs.TabLayout
import android.util.Log

class HistoryLayananFragment : Fragment() {

    // ==================== BDD CONTEXT ====================
    private data class HistoryScenarioContext(
        var userIsAtHistoryPage: Boolean = false,
        var selectedHistoryType: HistoryType = HistoryType.NONE,
        var hasServiceHistory: Boolean = false,
        var historyViewResult: HistoryViewResult = HistoryViewResult.PENDING
    )

    private enum class HistoryType {
        NONE, SERVICE, ITEM_BORROWING
    }

    private enum class HistoryViewResult {
        PENDING, SUCCESS_WITH_DATA, SUCCESS_EMPTY_STATE
    }

    private val scenarioContext = HistoryScenarioContext()

    // ==================== ORIGINAL PROPERTIES ====================
    private var _binding: FragmentHistoryLayananBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ServiceHistoryPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryLayananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BDD: GIVEN - User berada di halaman riwayat layanan
        givenUserIsAtServiceHistoryPage()

        setupToolbar()
        setupTabLayout()
        setupViewPager()
        setupTabBehavior()
        setupFragmentCommunication()

        // BDD: WHEN - User membuka halaman riwayat layanan
        whenUserOpensServiceHistoryPage()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User berada di halaman riwayat layanan
     */
    private fun givenUserIsAtServiceHistoryPage() {
        scenarioContext.userIsAtHistoryPage = true
        scenarioContext.selectedHistoryType = HistoryType.SERVICE
        scenarioContext.historyViewResult = HistoryViewResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at service history page")
    }

    /**
     * WHEN: User membuka halaman riwayat layanan
     */
    private fun whenUserOpensServiceHistoryPage() {
        if (!scenarioContext.userIsAtHistoryPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at history page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User opens service history page")
        checkUserServiceHistory()
    }

    /**
     * THEN: User melihat status terkini dari layanan (Skenario 1)
     */
    private fun thenUserSeesCurrentServiceStatus() {
        if (scenarioContext.hasServiceHistory) {
            scenarioContext.historyViewResult = HistoryViewResult.SUCCESS_WITH_DATA
            Log.d(TAG, "BDD - THEN: User sees current service status with data")
        }
    }

    /**
     * THEN: User melihat pesan "Belum ada riwayat layanan" (Skenario 2)
     */
    private fun thenUserSeesNoServiceHistoryMessage() {
        if (!scenarioContext.hasServiceHistory) {
            scenarioContext.historyViewResult = HistoryViewResult.SUCCESS_EMPTY_STATE
            Log.d(TAG, "BDD - THEN: User sees 'no service history' message")
        }
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun checkUserServiceHistory() {
        scenarioContext.hasServiceHistory = hasAnyServiceHistory()

        if (scenarioContext.hasServiceHistory) {
            thenUserSeesCurrentServiceStatus()
        } else {
            thenUserSeesNoServiceHistoryMessage()
        }
    }

    private fun hasAnyServiceHistory(): Boolean {
        // TODO: Implement actual check to repository/database
        return true
    }

    // ==================== ORIGINAL METHODS (UNCHANGED) ====================

    private fun setupToolbar() {
        binding.toolbar.apply {
            navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            setNavigationOnClickListener {
                findNavController().navigate(R.id.action_historyLayananFragment_to_historyFragment)
            }
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.setSelectedTabIndicatorColor(Color.TRANSPARENT)

        val tabTitles = listOf("Draft", "Terkirim", "In-Review", "Diterima", "Proses Pengerjaan", "Ditolak", "Selesai")
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
        adapter = ServiceHistoryPageAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
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

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }

    private fun setupFragmentCommunication() {
        binding.viewPager2.post {
            setupDraftToSentCommunication()
        }
    }

    private fun setupDraftToSentCommunication() {
        try {
            val draftFragment = childFragmentManager.findFragmentByTag("f0") as? DraftServiceFragment

            if (draftFragment != null) {
                draftFragment.setOnDataChangedListener(object : DraftServiceFragment.OnDataChangedListener {
                    override fun onDataChanged() {
                        Log.d(TAG, "Data changed notification received")
                        refreshSentServiceFragment()
                    }
                })
                Log.d(TAG, "Successfully setup communication with DraftServiceFragment")
            } else {
                Log.w(TAG, "DraftServiceFragment not found, retrying...")
                binding.viewPager2.postDelayed({
                    setupDraftToSentCommunication()
                }, 500)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up fragment communication", e)
        }
    }

    private fun refreshSentServiceFragment() {
        try {
            val sentFragment = childFragmentManager.findFragmentByTag("f1") as? SentServiceFragment

            if (sentFragment != null) {
                sentFragment.refreshData()
                Log.d(TAG, "Successfully refreshed SentServiceFragment")
            } else {
                Log.w(TAG, "SentServiceFragment not found")
                val fragments = childFragmentManager.fragments
                for (fragment in fragments) {
                    if (fragment is SentServiceFragment) {
                        fragment.refreshData()
                        Log.d(TAG, "Found and refreshed SentServiceFragment via iteration")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing SentServiceFragment", e)
        }
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
                cornerRadius = 24f
            }

            textView.background = drawable
            textView.elevation = 4f
            when (textView.text) {
                "Selesai" -> {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                }
                "Ditolak" -> {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
                else -> {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                }
            }
        }
    }

    private fun setSelectedTabStyle(tabTextView: TextView?) {
        tabTextView?.let { textView ->
            val drawable = GradientDrawable().apply {
                when (textView.text) {
                    "Selesai" -> setColor(ContextCompat.getColor(requireContext(), R.color.green))
                    "Ditolak" -> setColor(ContextCompat.getColor(requireContext(), R.color.red))
                    else -> setColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                }
                cornerRadius = 24f
            }

            textView.background = drawable
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.elevation = 6f
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
        setupDraftToSentCommunication()
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

    companion object {
        private const val TAG = "HistoryLayananFragment"
    }
}