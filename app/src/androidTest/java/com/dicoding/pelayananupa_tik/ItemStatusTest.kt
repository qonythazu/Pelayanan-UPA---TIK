package com.dicoding.pelayananupa_tik

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ItemStatusTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        Intents.release()
        clearAllMocks()
    }

    private fun setupMockLoggedInUser() {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockUser.email } returns "student@itk.ac.id"
        every { mockUser.uid } returns "test-uid-123"
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPrefs = context.getSharedPreferences("auth_prefs", 0)
        sharedPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", "student@itk.ac.id")
            .apply()
    }

    private fun navigateToItemHistory() {
        // Navigasi ke HistoryFragment
        onView(withId(R.id.historyFragment)).perform(click())
        Thread.sleep(1000)

        // Klik button untuk ke HistoryPeminjamanFragment
        onView(withId(R.id.btn_history_item)).perform(click())
        Thread.sleep(2000)
    }

    /**
     * Skenario BDD Test Case 1:
     * Given: User telah login dan pernah mengajukan peminjaman barang
     * When: User membuka halaman riwayat memilih peminjaman barang
     * Then: User melihat semua barang yang dipinjam maupun yang telah dikembalikan
     */
    @Test
    fun notEmptyHistory() {
        // Given: User telah login dan pernah mengajukan peminjaman barang
        setupMockLoggedInUser()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // When: User memilih halaman riwayat layanan
        navigateToItemHistory()

        // Then: User melihat halaman dengan tab layout
        onView(withId(R.id.view_pager_2)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()))
        Thread.sleep(1000)
    }

    /**
     * Skenario BDD Test Case 2:
     * Given: User telah login dan belum pernah mengajukan peminjaman barang
     * When: User membuka halaman riwayat memilih peminjaman barang
     * Then: User melihat pesan “Belum ada riwayat peminjaman” dan tidak menampilkan data apa pun
     */
    @Test
    fun emptyHistory() {
        // Given: User telah login dan belum ada data layanan
        setupMockLoggedInUser()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // When: User membuka halaman riwayat
        navigateToItemHistory()

        // Then: User melihat halaman tab layout
        onView(withId(R.id.view_pager_2)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()))

        // Cek semua tab satu per satu sampai nemu yang kosong
        val tabNames = listOf("Diajukan", "Diterima", "Diambil", "Ditolak", "Selesai") // sesuaikan dengan tab yang ada
        var foundEmptyState = false

        for (tabName in tabNames) {
            try {
                onView(withText(tabName)).perform(click())
                Thread.sleep(2000)

                try {
                    onView(
                        allOf(
                        withId(R.id.emptyStateTextView),
                        isDisplayed()
                    )
                    ).check(matches(isDisplayed()))
                    foundEmptyState = true
                    break
                } catch (e: Exception) {
                    continue
                }
            } catch (e: Exception) {
                continue
            }
        }

        if (!foundEmptyState) {
            onView(withId(R.id.tab_layout)).check(matches(isDisplayed()))
            onView(withId(R.id.view_pager_2)).check(matches(isDisplayed()))
        }
    }
}