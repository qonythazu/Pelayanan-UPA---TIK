package com.dicoding.pelayananupa_tik.fragment.peminjamanFeature

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
import com.dicoding.pelayananupa_tik.utils.UserManager
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

    // ==================== BDD CONTEXT ====================
    private data class ViewItemsScenarioContext(
        var userIsLoggedIn: Boolean = false,
        var userIsOnPeminjamanPage: Boolean = false,
        var itemsLoadResult: ItemsLoadResult = ItemsLoadResult.PENDING,
        var availableItemsCount: Int = 0
    )

    private data class AddToCartScenarioContext(
        var userIsLoggedIn: Boolean = false,
        var userIsOnPeminjamanPage: Boolean = false,
        var userClickedAddButton: Boolean = false,
        var selectedItem: Barang? = null,
        var addToCartResult: AddToCartResult = AddToCartResult.PENDING
    )

    private enum class ItemsLoadResult {
        PENDING, SUCCESS, FAILED, EMPTY
    }

    private enum class AddToCartResult {
        PENDING, SUCCESS, ALREADY_EXISTS, FAILED
    }

    private val viewItemsScenarioContext = ViewItemsScenarioContext()
    private val addToCartScenarioContext = AddToCartScenarioContext()

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

        // BDD: GIVEN - Setup initial conditions
        givenUserIsLoggedInAndOnPeminjamanPage()

        setupToolbar()
        setupRecyclerView()

        // BDD: Load items to display available items for borrowing
        loadAvailableItemsForViewing()

        setupSearch()
        observeBoxCount()
        observeRefreshNeeded()
    }

    // ==================== BDD VIEWING ITEMS SCENARIO METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman peminjaman barang
     */
    private fun givenUserIsLoggedInAndOnPeminjamanPage() {
        viewItemsScenarioContext.userIsLoggedIn = UserManager.isUserLoggedIn()
        viewItemsScenarioContext.userIsOnPeminjamanPage = true
        viewItemsScenarioContext.itemsLoadResult = ItemsLoadResult.PENDING

        // For add to cart scenario
        addToCartScenarioContext.userIsLoggedIn = viewItemsScenarioContext.userIsLoggedIn
        addToCartScenarioContext.userIsOnPeminjamanPage = true
        addToCartScenarioContext.addToCartResult = AddToCartResult.PENDING

        Log.d(TAG, "BDD - GIVEN: User is logged in (${viewItemsScenarioContext.userIsLoggedIn}) and on peminjaman page")
    }

    /**
     * THEN: User melihat semua barang yang tersedia untuk dipinjam
     */
    private fun thenUserSeesAllAvailableItemsForBorrowing() {
        viewItemsScenarioContext.itemsLoadResult = ItemsLoadResult.SUCCESS
        viewItemsScenarioContext.availableItemsCount = availableList.size

        Log.d(TAG, "BDD - THEN: User sees ${viewItemsScenarioContext.availableItemsCount} available items for borrowing")

        // Update UI to show items
        adapter.updateList(availableList)

        // Optional: Show success message if needed
        if (availableList.isNotEmpty()) {
            Log.d(TAG, "Successfully displaying ${availableList.size} available items")
        }
    }

    /**
     * THEN: User melihat pesan bahwa tidak ada barang tersedia
     */
    private fun thenUserSeesNoAvailableItemsMessage() {
        viewItemsScenarioContext.itemsLoadResult = ItemsLoadResult.EMPTY
        viewItemsScenarioContext.availableItemsCount = 0

        Log.d(TAG, "BDD - THEN: User sees no available items message")

        // Update UI to show empty state
        adapter.updateList(emptyList())
        showInfoToast("Tidak ada barang yang tersedia saat ini")
    }

    /**
     * THEN: User melihat pesan error saat gagal memuat barang
     */
    private fun thenUserSeesErrorLoadingItems() {
        viewItemsScenarioContext.itemsLoadResult = ItemsLoadResult.FAILED

        Log.d(TAG, "BDD - THEN: User sees error loading items")
        showErrorToast("Gagal memuat daftar barang")
    }

    // ==================== BDD ADD TO CART SCENARIO METHODS ====================

    /**
     * WHEN: User menekan tombol add pada barang untuk dimasukkan ke dalam keranjang
     */
    private fun whenUserClicksAddButtonForItem(barang: Barang) {
        if (!addToCartScenarioContext.userIsLoggedIn || !addToCartScenarioContext.userIsOnPeminjamanPage) {
            Log.e(TAG, "BDD - Precondition failed: User not logged in or not on peminjaman page")
            return
        }

        addToCartScenarioContext.userClickedAddButton = true
        addToCartScenarioContext.selectedItem = barang

        Log.d(TAG, "BDD - WHEN: User clicks add button for item: ${barang.namaBarang}")

        // Perform add to cart operation
        performAddToCart(barang)
    }

    /**
     * THEN: Barang ditambahkan ke keranjang peminjaman dan user melihat pesan konfirmasi
     */
    private fun thenItemAddedToCartWithConfirmationMessage(barang: Barang) {
        addToCartScenarioContext.addToCartResult = AddToCartResult.SUCCESS

        Log.d(TAG, "BDD - THEN: Item '${barang.namaBarang}' added to cart with confirmation message")

        // Show success message
        showSuccessToast("${barang.namaBarang} ditambahkan ke keranjang")
    }

    /**
     * THEN: User melihat pesan bahwa barang sudah ada di keranjang
     */
    private fun thenUserSeesItemAlreadyInCartMessage(barang: Barang) {
        addToCartScenarioContext.addToCartResult = AddToCartResult.ALREADY_EXISTS

        Log.d(TAG, "BDD - THEN: User sees item '${barang.namaBarang}' already in cart message")

        // Show info message
        showInfoToast("${barang.namaBarang} sudah ada di keranjang")
    }

    /**
     * THEN: User melihat pesan error saat gagal menambahkan ke keranjang
     */
    private fun thenUserSeesAddToCartErrorMessage(barang: Barang) {
        addToCartScenarioContext.addToCartResult = AddToCartResult.FAILED

        Log.d(TAG, "BDD - THEN: User sees add to cart error for item: ${barang.namaBarang}")

        // Show error message
        showErrorToast("Gagal menambahkan ${barang.namaBarang} ke keranjang")
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun loadAvailableItemsForViewing() {
        Log.d(TAG, "Loading available items for viewing...")
        getBarangDariFirestore()
    }

    private fun performAddToCart(barang: Barang) {
        try {
            if (boxViewModel.isItemInBox(barang)) {
                // BDD: THEN - Item already in cart
                thenUserSeesItemAlreadyInCartMessage(barang)
            } else {
                boxViewModel.addToBox(barang)
                // BDD: THEN - Item successfully added to cart
                thenItemAddedToCartWithConfirmationMessage(barang)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding item to cart: ${e.message}")
            // BDD: THEN - Failed to add to cart
            thenUserSeesAddToCartErrorMessage(barang)
        }
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
            Log.w(TAG, "Menu item R.id.menu_box not found")
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

        Log.d(TAG, "Badge updated: $boxCount items")
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(availableList, { barang ->
            // BDD: WHEN - User clicks add button for item
            whenUserClicksAddButtonForItem(barang)
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

                    // BDD: THEN - Handle the result based on items found
                    if (availableList.isNotEmpty()) {
                        thenUserSeesAllAvailableItemsForBorrowing()
                    } else {
                        thenUserSeesNoAvailableItemsMessage()
                    }

                    // Clear search if there's an active query
                    if (!binding.searchView.query.isNullOrEmpty()) {
                        binding.searchView.setQuery("", false)
                        binding.searchView.clearFocus()
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreFetch", "Error processing Firestore data: ${e.message}")
                    thenUserSeesErrorLoadingItems()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreFetch", "Gagal ambil data: ${e.message}", e)
                thenUserSeesErrorLoadingItems()
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

    private fun observeBoxCount() {
        boxViewModel.boxCount.observe(viewLifecycleOwner) { count ->
            updateBadge()
            Log.d(TAG, "Box count changed: $count")
        }
    }

    private fun refreshBarangData() {
        Log.d(TAG, "Refreshing barang data...")
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
            Log.e(TAG, "Error in onResume: ${e.message}")
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
            Log.e(TAG, "Error in onPause: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        badgeTextView = null
    }

    companion object {
        private const val TAG = "ProductListFragment"
    }
}