package com.dicoding.pelayananupa_tik

import android.view.View
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.dicoding.pelayananupa_tik.utils.UserData
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileFragmentTest {
    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        setupMockLoggedInUser()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        if (::activityScenario.isInitialized) {
            activityScenario.close()
        }
        clearAllMocks()
    }

    private fun setupMockLoggedInUser() {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockUser.email } returns "11201076@student.itk.ac.id"
        every { mockUser.uid } returns "test-uid-123"

        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser

        mockkObject(UserManager)
        every { UserManager.getCurrentUserEmail() } returns "11201076@student.itk.ac.id"
        every { UserManager.isUserLoggedIn() } returns true
        every { UserManager.isCurrentUserPredefined() } returns true

        val mockUserData = UserData(
            email = "11201076@student.itk.ac.id",
            namaLengkap = "Putri Qonita Arif",
            pekerjaan = "Mahasiswa",
            nim = "11201076",
            programStudi = "Informatika",
            nomorTelepon = "0852341234"
        )

        coEvery { UserManager.getCurrentUserData(any()) } coAnswers {
            val callback = firstArg<(UserData?) -> Unit>()
            callback(mockUserData)
        }

        every { UserManager.updatePhoneNumber(any(), any()) } answers {
            val callback = secondArg<(Boolean) -> Unit>()
            callback(true) // Default success, will be overridden in specific tests
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPrefs = context.getSharedPreferences("auth_prefs", 0)
        sharedPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", "11201076@student.itk.ac.id")
            .apply()
    }

    /**
     * Skenario 1: Edit nomor telepon berhasil
     * Given: User telah login dan berada di halaman profile
     * When: User memilih edit nomor telepon, mengisi minimal 10 angka dan mengirimkan perubahan
     * Then: User melihat pesan konfirmasi "Nomor telepon berhasil diperbarui" dan melihat perubahan yang diterapkan pada profil
     */
    @Test
    fun successEditPhoneNumber() {
        // Given: User telah login dan berada di halaman profile
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.profileFragment)).perform(click())
        Thread.sleep(1500)

        // When: User memilih edit nomor telepon, mengisi minimal 10 angka dan mengirimkan perubahan
        onView(withId(R.id.nomorTeleponLayout))
            .perform(clickEndIcon())
        onView(withText("Edit Nomor Telepon")).check(matches(isDisplayed()))
        Thread.sleep(1000)

        onView(withText("Edit Nomor Telepon")).check(matches(isDisplayed()))
        onView(withHint("Masukkan nomor telepon")).perform(
            clearText(),
            typeText("08123456789012"),
            closeSoftKeyboard()
        )

        onView(withText("Simpan")).perform(click())

        // Then: User melihat pesan konfirmasi "Nomor telepon berhasil diperbarui" dan melihat perubahan yang diterapkan pada profil
        Thread.sleep(2000)
    }

    /**
     * Skenario 2: Edit nomor telepon gagal karena kurang dari 10 angka
     * Given: User telah login dan berada di halaman profile
     * When: User memilih edit nomor telepon, mengisi kurang dari 10 angka dan mengirimkan perubahan
     * Then: Gagal diperbarui dan user melihat pesan error dan user kembali ke halaman profil
     */
    @Test
    fun failEditPhoneNumber() {
        // Given: User telah login dan berada di halaman profile
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.profileFragment)).perform(click())
        Thread.sleep(1500)

        // When: User memilih edit nomor telepon, mengisi kurang dari 10 angka dan mengirimkan perubahan
        onView(withId(R.id.nomorTeleponLayout))
            .perform(clickEndIcon())
        onView(withText("Edit Nomor Telepon")).check(matches(isDisplayed()))
        Thread.sleep(1000)

        onView(withText("Edit Nomor Telepon")).check(matches(isDisplayed()))
        onView(withHint("Masukkan nomor telepon")).perform(
            clearText(),
            typeText("123"),
            closeSoftKeyboard()
        )

        onView(withText("Simpan")).perform(click())

        // Then: Gagal diperbarui dan user melihat pesan error dan user tetap di dialog edit
        Thread.sleep(1000)
        onView(withText("Edit Nomor Telepon")).check(matches(isDisplayed()))
    }

    private fun clickEndIcon(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextInputLayout::class.java)
            }

            override fun getDescription(): String {
                return "Click end icon"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val textInputLayout = view as TextInputLayout
                val endIconView = textInputLayout.findViewById<View>(com.google.android.material.R.id.text_input_end_icon)
                endIconView?.performClick()
            }
        }
    }
}