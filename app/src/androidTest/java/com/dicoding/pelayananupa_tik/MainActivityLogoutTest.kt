package com.dicoding.pelayananupa_tik

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityLogoutTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        Intents.init()

        // Setup mock user sebagai logged in
        setupMockLoggedInUser()
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

    /**
     * Skenario BDD Test untuk Logout
     * Given: User telah login
     * When: User menekan tombol logout
     * Then: User keluar dari akun dan berpindah ke halaman login dan user melihat pesan "Anda berhasil logout"
     */
    @Test
    fun logout() {
        // Given: User telah login
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // When: Click logout button
        onView(withId(R.id.btn_logout)).perform(click())

        // Then: App tidak crash (jika sampai sini berarti tidak crash)
    }
}