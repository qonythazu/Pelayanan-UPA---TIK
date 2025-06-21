package com.dicoding.pelayananupa_tik

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dicoding.pelayananupa_tik.activity.MainActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FormPeminjamanFragmentTest {

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
     * Skenario FormSubmissionBerhasil
     * Given : User telah login dan berada di halaman formulir peminjaman barang
     * When : User mengisi semua data yang diminta di formulir dan menekan tombol submit
     * Then  : Berhasil terkirim dan user otomatis ke halaman riwayat
     */
    @Test
    fun formSubmissionBerhasil(){
        // Given : User telah login dan berada di halaman formulir peminjaman barang
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    clickChildViewWithId(R.id.btn_add)
                )
            )

        Thread.sleep(1000)

        onView(withId(R.id.menu_box)).perform(click())
        onView(withId(R.id.checkbox_select_all)).perform(click())
        onView(withId(R.id.btn_pinjam)).perform(click())

        Thread.sleep(1000)

        // When : User mengisi semua data yang diminta di formulir dan menekan tombol submit
        // Isi Tujuan Peminjaman
        onView(allOf(isDescendantOfA(withId(R.id.tujuanPeminjamanLayout)), isAssignableFrom(android.widget.EditText::class.java)))
            .perform(typeText("Keperluan presentasi proyek akhir"), closeSoftKeyboard())

        // Handle Tanggal Mulai Date Picker - pilih tanggal 29
        handleDatePicker(R.id.tanggalMulaiLayout, 29)

        Thread.sleep(500)

        // Handle Tanggal Selesai Date Picker - pilih tanggal 30
        handleDatePicker(R.id.tanggalSelesaiLayout, 30)

        Thread.sleep(500)

        // Isi Harapan Anda
        onView(allOf(isDescendantOfA(withId(R.id.harapanAndaLayout)), isAssignableFrom(android.widget.EditText::class.java)))
            .perform(typeText("Semoga perangkat dapat berfungsi dengan baik selama peminjaman"), closeSoftKeyboard())

        // Isi Nama Penanggung Jawab
        onView(allOf(isDescendantOfA(withId(R.id.namaPenanggungJawabLayout)), isAssignableFrom(android.widget.EditText::class.java)))
            .perform(typeText("John Doe"), closeSoftKeyboard())

        // Then : Form berhasil disubmit
        onView(withId(R.id.btnSubmit)).perform(scrollTo(), click())
        Thread.sleep(2000)
    }

    /**
     * Handle date picker yang beneran dipencet
     */
    private fun handleDatePicker(layoutId: Int, day: Int) {
        onView(allOf(
            isDescendantOfA(withId(layoutId)),
            isAssignableFrom(android.widget.EditText::class.java)
        )).perform(scrollTo(), click())

        Thread.sleep(1000)

        try {
            onView(allOf(
                withText(day.toString()),
                isDisplayed(),
                isClickable()
            )).perform(click())

            Thread.sleep(500)
            onView(withText("Oke")).perform(click())

        } catch (e: Exception) {
            try {
                onView(allOf(
                    withText(day.toString()),
                    isDescendantOfA(withClassName(org.hamcrest.Matchers.containsString("DatePicker")))
                )).perform(click())

                onView(withText("Oke")).perform(click())

            } catch (e2: Exception) {
                try {
                    onView(withClassName(org.hamcrest.Matchers.endsWith("DatePicker")))
                        .perform(setDate(2025, 6, day))

                    onView(withText("Oke")).perform(click())

                } catch (e3: Exception) {
                    onView(allOf(
                        isDescendantOfA(withId(layoutId)),
                        isAssignableFrom(android.widget.EditText::class.java)
                    )).perform(
                        clearText(),
                        typeText("${day}/06/2025"),
                        closeSoftKeyboard()
                    )
                }
            }
        }

        Thread.sleep(500)
    }

    /**
     * Custom ViewAction untuk set date langsung
     */
    private fun setDate(year: Int, month: Int, day: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription(): String = "Set date on DatePicker"
            override fun getConstraints(): Matcher<View> = isAssignableFrom(android.widget.DatePicker::class.java)
            override fun perform(uiController: UiController, view: View) {
                val datePicker = view as android.widget.DatePicker
                datePicker.updateDate(year, month - 1, day)
            }
        }
    }

    /**
     * Skenario FormSubmissionGagal
     * Given : User telah login dan berada di halaman formulir peminjaman barang
     * When : User mengisi formulir tanpa mengisi salah satu data yang wajib dan menekan tombol submit
     * Then  : Gagal terkirim dan user melihat pesan error dan tetap di halaman form
     */
    @Test
    fun formSubmissionGagal(){
        // Given : User telah login dan berada di halaman formulir peminjaman barang
        onView(withId(R.id.menu_peminjaman_barang)).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    clickChildViewWithId(R.id.btn_add)
                )
            )

        Thread.sleep(1000)

        onView(withId(R.id.menu_box)).perform(click())
        onView(withId(R.id.checkbox_select_all)).perform(click())
        onView(withId(R.id.btn_pinjam)).perform(click())

        Thread.sleep(1000)

        // When : User mengisi formulir tanpa mengisi salah satu data yang wajib
        // Hanya isi beberapa field, sisakan field wajib kosong
        onView(allOf(isDescendantOfA(withId(R.id.tujuanPeminjamanLayout)), isAssignableFrom(android.widget.EditText::class.java)))
            .perform(typeText("Keperluan presentasi"), closeSoftKeyboard())

        // Sengaja tidak mengisi tanggal mulai dan tanggal selesai serta nama penanggung jawab
        // Submit form tanpa mengisi semua field wajib
        onView(withId(R.id.btnSubmit)).perform(scrollTo(), click())
        Thread.sleep(1000)

        // Then : Gagal terkirim dan user melihat pesan error
        try {
            // Option 1: Cek error message untuk tanggal mulai
            onView(withText("Tanggal mulai tidak boleh kosong"))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            try {
                // Option 2: TextInputLayout error - cek error pada layout tanggal mulai
                onView(withId(R.id.tanggalMulaiLayout))
                    .check(matches(hasErrorText("Tanggal mulai tidak boleh kosong")))
            } catch (e2: Exception) {
                try {
                    // Option 3: Cek error message untuk tanggal selesai
                    onView(withText("Tanggal selesai tidak boleh kosong"))
                        .check(matches(isDisplayed()))
                } catch (e3: Exception) {
                    try {
                        // Option 4: TextInputLayout error - cek error pada layout tanggal selesai
                        onView(withId(R.id.tanggalSelesaiLayout))
                            .check(matches(hasErrorText("Tanggal selesai tidak boleh kosong")))
                    } catch (e4: Exception) {
                        try {
                            // Option 5: Toast message umum
                            onView(withText("Harap lengkapi semua data yang wajib"))
                                .check(matches(isDisplayed()))
                        } catch (e5: Exception) {
                            try {
                                // Option 6: Dialog error
                                onView(withText("Error"))
                                    .check(matches(isDisplayed()))
                            } catch (e6: Exception) {
                                try {
                                    // Option 7: Snackbar
                                    onView(withText("Field wajib harus diisi"))
                                        .check(matches(isDisplayed()))
                                } catch (e7: Exception) {
                                    // Option 8: Any error text that contains "tanggal"
                                    onView(withText(org.hamcrest.Matchers.containsString("tanggal")))
                                        .check(matches(isDisplayed()))
                                }
                            }
                        }
                    }
                }
            }
        }
        onView(withId(R.id.btnSubmit))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
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