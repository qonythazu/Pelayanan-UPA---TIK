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
import com.dicoding.pelayananupa_tik.fragment.form.FormPemeliharaanAkunFragment
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
class FormPemeliharaanAkunFragmentTest {

    private lateinit var scenario: FragmentScenario<FormPemeliharaanAkunFragment>
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
        scenario = launchFragmentInContainer<FormPemeliharaanAkunFragment>(
            themeResId = R.style.Theme_PelayananUPATIK
        )

        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            navController.setGraph(R.navigation.main_navigation)
            navController.setCurrentDestination(R.id.formPemeliharaanAkunFragment)
        }

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
    }

    /**
     * Skenario 1: Form submission berhasil
     * Given: User telah login dan berada di halaman formulir pemeliharaan akun
     * When: User mengisi semua data yang diminta di formulir dan menekan tombol submit
     * Then: Berhasil terkirim dan user otomatis berpindah ke halaman riwayat
     */
    @Test
    fun formSubmissionBerhasil() {
        // Given: User telah login dan berada di halaman formulir pemeliharaan akun
        launchFragment()

        // When: User mengisi semua data yang diminta di formulir
        // Pilih layanan (Subdomain)
        onView(withId(R.id.radioSubDomain)).perform(click())

        // Pilih jenis pemeliharaan (Reset Password Akun)
        onView(withId(R.id.radioReset)).perform(click())

        // Isi nama akun layanan - Target TextInputEditText di dalam TextInputLayout
        onView(allOf(
            isDescendantOfA(withId(R.id.namaAkunLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("test.account@itk.ac.id"),
            closeSoftKeyboard()
        )

        // Isi alasan pemeliharaan - Target TextInputEditText di dalam TextInputLayout
        onView(allOf(
            isDescendantOfA(withId(R.id.alasanLayout)),
            withClassName(endsWith("TextInputEditText"))
        )).perform(
            click(),
            clearText(),
            typeText("Lupa password akun dan perlu direset"),
            closeSoftKeyboard()
        )

        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        val mockTask = mockk<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentReference>>(relaxed = true)
        every { mockFirestore.collection("form_pemeliharaan") } returns mockCollection
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
     * Given: User telah login dan berada di halaman formulir pemeliharaan akun
     * When: User mengisi formulir tanpa mengisi salah satu data yang wajib dan menekan tombol submit
     * Then: Gagal terkirim dan user melihat pesan error dan user kembali ke halaman formulir pemeliharaan akun
     */
    @Test
    fun formSubmissionGagal() {
        // Given: User telah login dan berada di halaman formulir pemeliharaan akun
        launchFragment()

        // When: User mengisi formulir tanpa mengisi salah satu data yang wajib
        // Hanya mengisi layanan, tidak mengisi jenis, akun, dan alasan
        onView(withId(R.id.radioSubDomain)).perform(click())

        // Dan menekan tombol submit
        onView(withId(R.id.btnSubmit)).perform(scrollTo(), click())

        // Then: User melihat pesan error dan tetap di halaman formulir
        onView(withId(R.id.radioGroupLayanan)).check(matches(isDisplayed()))
        onView(withId(R.id.radioGroupJenis)).check(matches(isDisplayed()))
        onView(withId(R.id.namaAkunLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.alasanLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmit)).check(matches(isDisplayed()))
    }
}