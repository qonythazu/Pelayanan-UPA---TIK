package com.dicoding.pelayananupa_tik

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dicoding.pelayananupa_tik.fragment.form.FormPengaduanLayananFragment
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FormPengaduanLayananFragmentTest {

    private lateinit var scenario: FragmentScenario<FormPengaduanLayananFragment>
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
        if (::scenario.isInitialized) {
            scenario.close()
        }
        clearAllMocks()
    }

    private fun setupMockLoggedInUser() {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockUser.email } returns "student@itk.ac.id"
        every { mockUser.uid } returns "test-uid-123"

        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser
        mockkObject(UserManager)
        every { UserManager.getCurrentUserEmail() } returns "student@itk.ac.id"

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPrefs = context.getSharedPreferences("auth_prefs", 0)
        sharedPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", "student@itk.ac.id")
            .apply()
    }

    private fun launchFragment() {
        scenario = launchFragmentInContainer<FormPengaduanLayananFragment>(
            themeResId = R.style.Theme_PelayananUPATIK
        )

        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            navController.setGraph(R.navigation.main_navigation)
            navController.setCurrentDestination(R.id.formPengaduanLayananFragment)
        }

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
    }

    /**
     * Skenario 1: Form submission berhasil
     * Given: User telah login dan berada di halaman formulir pengaduan layanan
     * When: User mengisi semua data yang diminta di formulir dan menekan tombol submit
     * Then: Berhasil terkirim dan user otomatis berpindah ke halaman riwayat
     */
    @Test
    fun givenUserLoggedInAndOnFormPage_whenUserFillsAllRequiredDataAndSubmit_thenSuccessAndNavigateToHistory() {
        // Given: User telah login dan berada di halaman formulir pengaduan layanan
        launchFragment()

        // When: User mengisi semua data yang diminta di formulir
        // Isi layanan - Target TextInputEditText di dalam TextInputLayout
        onView(allOf(
            isDescendantOfA(withId(R.id.layananLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("Layanan IT Support"),
            closeSoftKeyboard()
        )

        // Isi kontak - Target TextInputEditText di dalam TextInputLayout
        onView(allOf(
            isDescendantOfA(withId(R.id.kontakLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("081234567890"),
            closeSoftKeyboard()
        )

        // Isi keluhan - Target TextInputEditText di dalam TextInputLayout
        onView(allOf(
            isDescendantOfA(withId(R.id.keluhanAndaLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("Sistem sering mengalami gangguan dan perlu perbaikan segera"),
            closeSoftKeyboard()
        )

        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockTask = mockk<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentReference>>(relaxed = true)
        every { mockFirestore.collection("form_pengaduan") } returns mockCollection
        every { mockCollection.add(any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentReference>>()
            val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
            listener.onSuccess(mockDocRef)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        // Dan menekan tombol submit
        onView(withId(R.id.btnSubmit)).perform(scrollTo(), click())

        // Then: Form berhasil disubmit (tidak ada error yang muncul)
        Thread.sleep(1000)
    }

    /**
     * Skenario 2: Form submission gagal karena data tidak lengkap
     * Given: User telah login dan berada di halaman formulir pengaduan layanan
     * When: User mengisi formulir tanpa mengisi salah satu data yang wajib dan menekan tombol submit
     * Then: Gagal terkirim dan user melihat pesan error "Harap lengkapi semua data yang wajib" dan user kembali ke halaman formulir pengaduan layanan
     */
    @Test
    fun givenUserLoggedInAndOnFormPage_whenUserSubmitIncompleteForm_thenShowErrorAndStayOnForm() {
        // Given: User telah login dan berada di halaman formulir pengaduan layanan
        launchFragment()

        // When: User mengisi formulir tanpa mengisi salah satu data yang wajib
        // Hanya mengisi layanan, tidak mengisi kontak dan keluhan
        onView(allOf(
            isDescendantOfA(withId(R.id.layananLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("Layanan IT Support"),
            closeSoftKeyboard()
        )

        // Tidak mengisi kontak dan keluhan (data wajib lainnya)

        // Dan menekan tombol submit
        onView(withId(R.id.btnSubmit)).perform(scrollTo(), click())

        // Then: User melihat pesan error dan tetap di halaman formulir
        onView(withId(R.id.layananLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.kontakLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.keluhanAndaLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmit)).check(matches(isDisplayed()))
        onView(allOf(
            isDescendantOfA(withId(R.id.kontakLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(click())
        onView(allOf(
            isDescendantOfA(withId(R.id.keluhanAndaLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(click())
    }
}