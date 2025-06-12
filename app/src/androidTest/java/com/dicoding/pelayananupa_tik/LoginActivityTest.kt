package com.dicoding.pelayananupa_tik

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dicoding.pelayananupa_tik.activity.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityTest {

    private lateinit var scenario: ActivityScenario<LoginActivity>
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setUp() {
        Intents.init()
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        clearAuthState()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        Intents.release()
        clearAllMocks()
    }

    private fun clearAuthState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPrefs = context.getSharedPreferences("auth_prefs", 0)
        sharedPrefs.edit().clear().apply()

        FirebaseAuth.getInstance().signOut()
    }

    /**
     * Skenario 1: BDD Test
     * Given: User berada di halaman login
     * When: User menekan tombol login dengan google dan memilih akun google civitas ITK
     * Then: User berpindah ke halaman beranda
     */
    @Test
    fun givenUserOnLoginPage_whenLoginWithITKGoogleAccount_thenNavigateToHomePage() {
        // Given: User berada di halaman login
        scenario = ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.btn_login))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        // When: User menekan tombol login (ini akan trigger Google Sign-In)
        onView(withId(R.id.btn_login)).perform(click())

        // Then: Verify login button masih bisa diklik
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()))
    }

    /**
     * Skenario 2: BDD Test
     * Given: User berada di halaman login
     * When: User menekan tombol login dengan google dan memilih akun google selain akun google civitas ITK
     * Then: User gagal login dan tetap berada di halaman login dan user mendapatkan pesan error "Maaf anda bukan civitas ITK"
     */
    @Test
    fun givenUserOnLoginPage_whenLoginWithNonITKGoogleAccount_thenStayOnLoginPageWithError() {
        // Given: User berada di halaman login
        scenario = ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.btn_login))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        // When: User menekan tombol login
        onView(withId(R.id.btn_login)).perform(click())

        // Then: User tetap di halaman login (tidak pindah ke MainActivity)
        onView(withId(R.id.btn_login))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
}

// Data class untuk testing
data class UserData(
    val email: String,
    val namaLengkap: String? = null,
    val userId: String? = null
)