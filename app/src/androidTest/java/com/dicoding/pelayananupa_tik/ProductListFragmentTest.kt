package com.dicoding.pelayananupa_tik

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dicoding.pelayananupa_tik.activity.MainActivity
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProductListFragmentTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private var idlingResource: IdlingResource? = null

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        idlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
        scenario.close()
    }

    /**
     * Skenario melihat daftar barang yang dapat dipinjam
     * Given : User telah login
     * When : User memilih halaman peminjaman barang
     * Then  : User melihat semua barang yang tersedia untuk dipinjam
     */
    @Test
    fun givenUserLoggedIn_whenProductListClicked_thenItemsAreDisplayed() {
        // Given : User telah login
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // When : User memilih halaman peminjaman barang
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        // Wait for data to load from Firestore
        Thread.sleep(5000)

        // Then : User melihat semua barang yang tersedia untuk dipinjam
        onView(withId(R.id.recycler_view)) // Sekarang pakai recycler_view bukan recyclerView
            .check(matches(isDisplayed()))
    }

    /**
     * Skenario Menambahkan Barang ke Dalam Keranjang - Metode Sederhana
     * Given : User telah login dan berada di halaman peminjaman barang
     * When : User menekan tombol add pada barang pertama untuk dimasukkan ke dalam keranjang
     * Then  : Barang ditambahkan ke keranjang peminjaman dan user melihat pesan konfirmasi
     */
    @Test
    fun givenUserInProductList_whenAddButtonClicked_thenItemAddedToCart() {
        // Given: User is logged in and on product list
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        // Wait for data to load from Firestore
        Thread.sleep(8000)

        // When: User clicks on the add button of first item
        onView(withId(R.id.recycler_view)) // Pakai ID yang benar: recycler_view
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, // First item (index 0)
                    clickChildViewWithId(R.id.btn_add) // btn_add sudah benar
                )
            )

        // Then: Item should be added to cart
        Thread.sleep(2000)

        // Navigate to box to verify item was added
        onView(withId(R.id.menu_box)).perform(click())
    }

    /**
     * Alternative simpler test method - Click on first item directly
     */
    @Test
    fun givenUserInProductList_whenFirstItemClicked_thenItemAddedToCart() {
        // Given: User is logged in and on product list
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        // Wait for data to load
        Thread.sleep(8000)

        // When: User clicks on the first item card
        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    clickChildViewWithId(R.id.card_view_product) // Click on the card
                )
            )

        // Then: Item should be added to cart
        Thread.sleep(2000)
        onView(withId(R.id.menu_box)).perform(click())
    }

    /**
     * Test untuk memverifikasi search functionality
     */
    @Test
    fun givenUserInProductList_whenUserSearches_thenSearchViewWorks() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Navigate to product list
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        Thread.sleep(3000)

        // When: User clicks on search view (ID sudah benar: search_view)
        onView(withId(R.id.search_view))
            .perform(click())

        // Type search query
        onView(withId(R.id.search_view))
            .perform(typeText("Router"))

        Thread.sleep(2000)

        // Then: Search view should show the query
        // Note: Hasil pencarian bergantung pada data real dari Firestore
    }

    /**
     * Test khusus untuk verifikasi tombol Add bekerja
     */
    @Test
    fun givenUserInProductList_whenAddButtonClickedDirectly_thenButtonResponds() {
        // Given: User is on product list
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        // Wait for data to load
        Thread.sleep(8000)

        // When: User clicks add button on first item
        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    clickChildViewWithId(R.id.btn_add)
                )
            )

        // Then: Just wait and see if no crash occurs
        Thread.sleep(3000)
    }

    /**
     * Test untuk memastikan RecyclerView memiliki data
     */
    @Test
    fun givenUserInProductList_whenDataLoaded_thenRecyclerViewHasItems() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())

        // Wait for data to load
        Thread.sleep(10000)

        // Check if RecyclerView has items
        onView(withId(R.id.recycler_view))
            .check(matches(isDisplayed()))
            .perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0)
            )
    }

    /**
     * Helper function to click on child view within RecyclerView item
     */
    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(View::class.java)
            }

            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v?.performClick()
            }
        }
    }
}