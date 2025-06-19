package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.FragmentHomeBinding
import com.dicoding.pelayananupa_tik.utils.UserManager

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ==================== BDD CONTEXT ====================
    private data class NavigationScenarioContext(
        var userIsLoggedIn: Boolean = false,
        var userClickedPeminjamanMenu: Boolean = false,
        var navigationResult: NavigationResult = NavigationResult.PENDING
    )

    private enum class NavigationResult {
        PENDING, SUCCESS, FAILED
    }

    private val navigationScenarioContext = NavigationScenarioContext()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuNavigation()
    }

    // ==================== BDD PEMINJAMAN BARANG NAVIGATION METHODS ====================

    /**
     * GIVEN: User telah login
     */
    private fun givenUserIsLoggedIn() {
        navigationScenarioContext.userIsLoggedIn = UserManager.isUserLoggedIn()
        navigationScenarioContext.navigationResult = NavigationResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is logged in: ${navigationScenarioContext.userIsLoggedIn}")
    }

    /**
     * WHEN: User memilih halaman peminjaman barang
     */
    private fun whenUserSelectsPeminjamanBarangPage() {
        if (!navigationScenarioContext.userIsLoggedIn) {
            Log.e(TAG, "BDD - Precondition failed: User is not logged in")
            return
        }

        navigationScenarioContext.userClickedPeminjamanMenu = true
        Log.d(TAG, "BDD - WHEN: User selects peminjaman barang page")

        // Lakukan navigasi ke ProductListFragment
        navigateToPeminjamanBarang()
    }

    /**
     * THEN: User melihat semua barang yang tersedia untuk dipinjam
     */
    private fun thenUserSeesAvailableItemsForBorrowing() {
        navigationScenarioContext.navigationResult = NavigationResult.SUCCESS
        Log.d(TAG, "BDD - THEN: User successfully navigated to see available items for borrowing")

        // Navigation berhasil, ProductListFragment akan handle menampilkan barang
        // Logika "melihat barang yang tersedia" akan dihandle di ProductListFragment
    }

    /**
     * THEN: User gagal mengakses halaman peminjaman barang
     */
    private fun thenUserFailsToAccessPeminjamanPage() {
        navigationScenarioContext.navigationResult = NavigationResult.FAILED
        Log.d(TAG, "BDD - THEN: User fails to access peminjaman barang page")

        // Handle error case jika diperlukan
        // Misalnya tampilkan toast error atau redirect ke login
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun setupMenuNavigation() {
        binding.apply {
            // BDD Implementation untuk Peminjaman Barang
            menuPeminjamanBarang.setOnClickListener {
                // BDD: GIVEN - Pastikan user sudah login
                givenUserIsLoggedIn()

                // BDD: WHEN - User memilih halaman peminjaman barang
                whenUserSelectsPeminjamanBarangPage()
            }

            // Menu lainnya tetap seperti semula
            menuPembuatanWebDll.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPembuatanWebDllFragment)
            }
            menuPemeliharaanAkun.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPemeliharaanAkunFragment)
            }
            menuPengaduanLayanan.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPengaduanLayananFragment)
            }
            menuPemasanganPerangkat.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPemasanganPerangkatFragment)
            }
            menuBantuanOperatorTik.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formBantuanOperatorFragment)
            }
        }
    }

    private fun navigateToPeminjamanBarang() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_productListFragment)

            // BDD: THEN - Navigasi berhasil
            thenUserSeesAvailableItemsForBorrowing()

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to peminjaman barang: ${e.message}")

            // BDD: THEN - Navigasi gagal
            thenUserFailsToAccessPeminjamanPage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}