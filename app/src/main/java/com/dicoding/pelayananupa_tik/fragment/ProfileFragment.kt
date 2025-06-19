package com.dicoding.pelayananupa_tik.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.FragmentProfileBinding
import com.dicoding.pelayananupa_tik.helper.isValidPhoneNumber
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.dicoding.pelayananupa_tik.utils.UserData
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ==================== BDD CONTEXT ====================

    private data class EditPhoneScenarioContext(
        var userIsLoggedIn: Boolean = false,
        var userIsInProfilePage: Boolean = false,
        var userSelectedEditPhone: Boolean = false,
        var phoneInput: String = "",
        var validationResult: PhoneValidationResult = PhoneValidationResult.PENDING,
        var updateResult: PhoneUpdateResult = PhoneUpdateResult.PENDING
    )

    private enum class PhoneValidationResult {
        PENDING, VALID, INVALID
    }

    private enum class PhoneUpdateResult {
        PENDING, SUCCESS, FAILED
    }

    private val editPhoneScenarioContext = EditPhoneScenarioContext()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        setFieldsReadOnly()
        setupPhoneEditButton()
    }

    private fun setupPhoneEditButton() {
        binding.nomorTeleponLayout.setEndIconOnClickListener {
            // BDD: GIVEN - Pastikan user sudah login dan berada di halaman profile
            givenUserIsLoggedInAndInProfilePage()

            // BDD: WHEN - User memilih edit nomor telepon
            whenUserSelectsEditPhone()
        }
    }

    // ==================== BDD SKENARIO 1: USER BERHASIL MENGUBAH NOMOR TELEPON ====================

    /**
     * GIVEN: User telah login dan berada di halaman profile
     */
    private fun givenUserIsLoggedInAndInProfilePage() {
        editPhoneScenarioContext.userIsLoggedIn = UserManager.isUserLoggedIn()
        editPhoneScenarioContext.userIsInProfilePage = true
        editPhoneScenarioContext.validationResult = PhoneValidationResult.PENDING
        editPhoneScenarioContext.updateResult = PhoneUpdateResult.PENDING

        Log.d(TAG, "BDD - GIVEN: User is logged in: ${editPhoneScenarioContext.userIsLoggedIn}")
        Log.d(TAG, "BDD - GIVEN: User is in profile page: ${editPhoneScenarioContext.userIsInProfilePage}")
    }

    /**
     * WHEN: User memilih edit nomor telepon
     */
    private fun whenUserSelectsEditPhone() {
        if (!editPhoneScenarioContext.userIsLoggedIn || !editPhoneScenarioContext.userIsInProfilePage) {
            Log.e(TAG, "BDD - Precondition failed: User not logged in or not in profile page")
            return
        }

        editPhoneScenarioContext.userSelectedEditPhone = true
        Log.d(TAG, "BDD - WHEN: User selects edit phone")

        // Tampilkan dialog edit phone
        showEditPhoneDialog()
    }

    /**
     * WHEN: User mengisi minimal 10 angka dan mengirimkan perubahan
     */
    private fun whenUserEntersValidPhoneAndSubmits(phoneInput: String) {
        editPhoneScenarioContext.phoneInput = phoneInput
        Log.d(TAG, "BDD - WHEN: User enters phone input: $phoneInput")

        // Validasi nomor telepon
        if (isValidPhoneNumber(phoneInput)) {
            editPhoneScenarioContext.validationResult = PhoneValidationResult.VALID
            Log.d(TAG, "BDD - Phone validation: VALID")

            // Lakukan update nomor telepon
            performPhoneUpdate(phoneInput)
        } else {
            editPhoneScenarioContext.validationResult = PhoneValidationResult.INVALID
            Log.d(TAG, "BDD - Phone validation: INVALID")

            // BDD: THEN - Skenario 2 (gagal karena validasi)
            thenUserSeesValidationErrorAndStaysInDialog()
        }
    }

    /**
     * THEN: User melihat pesan konfirmasi "Nomor telepon berhasil diperbarui" dan melihat perubahan pada profil
     */
    private fun thenUserSeesSuccessMessageAndUpdatedProfile(newPhone: String) {
        editPhoneScenarioContext.updateResult = PhoneUpdateResult.SUCCESS

        Log.d(TAG, "BDD - THEN: User sees success message and updated profile")

        // Update UI dengan nomor telepon baru
        binding.nomorTeleponLayout.editText?.setText(newPhone)

        // Tampilkan pesan sukses
        Toast.makeText(requireContext(), "Nomor telepon berhasil diperbarui", Toast.LENGTH_SHORT).show()

        Log.d(TAG, "Phone number updated to: $newPhone")
    }

    // ==================== BDD SKENARIO 2: USER GAGAL MENGUBAH NOMOR TELEPON ====================

    /**
     * WHEN: User mengisi kurang dari 10 angka dan mengirimkan perubahan
     */
    private fun whenUserEntersInvalidPhoneAndSubmits(phoneInput: String) {
        editPhoneScenarioContext.phoneInput = phoneInput
        editPhoneScenarioContext.validationResult = PhoneValidationResult.INVALID

        Log.d(TAG, "BDD - WHEN: User enters invalid phone input: $phoneInput")

        // BDD: THEN - Langsung ke hasil gagal
        thenUserSeesValidationErrorAndStaysInDialog()
    }

    /**
     * THEN: Gagal diperbarui dan user melihat pesan error dan user kembali ke halaman profil
     */
    private fun thenUserSeesValidationErrorAndStaysInDialog() {
        Log.d(TAG, "BDD - THEN: User sees validation error and stays in dialog")

        // Error message akan ditampilkan dalam dialog (tidak menutup dialog)
        // Implementasi ada di showEditPhoneDialog()
    }

    /**
     * THEN: User melihat pesan error karena gagal update ke server
     */
    private fun thenUserSeesUpdateFailureAndReturnsToProfile() {
        editPhoneScenarioContext.updateResult = PhoneUpdateResult.FAILED

        Log.d(TAG, "BDD - THEN: User sees update failure and returns to profile")

        Toast.makeText(requireContext(), "Gagal memperbarui nomor telepon", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Failed to update phone number")
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun showEditPhoneDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "Masukkan nomor telepon"
            val currentPhone = binding.nomorTeleponLayout.editText?.text.toString()
            if (currentPhone != "Tidak tersedia") {
                setText(currentPhone)
                setSelection(text.length)
            }
        }

        val errorText = android.widget.TextView(requireContext()).apply {
            text = ""
            setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            textSize = 12f
            visibility = View.GONE
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(editText)
            addView(errorText)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Nomor Telepon")
            .setMessage("Minimal 10 digit angka")
            .setView(container)
            .setPositiveButton("Simpan", null) // Set null dulu
            .setNegativeButton("Batal", null)
            .create()

        // Override positive button click setelah dialog dibuat
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newPhone = editText.text.toString().trim()

                // BDD: WHEN - User mengisi dan mengirimkan perubahan
                if (isValidPhoneNumber(newPhone)) {
                    whenUserEntersValidPhoneAndSubmits(newPhone)
                    dialog.dismiss() // Tutup dialog hanya jika valid
                } else {
                    whenUserEntersInvalidPhoneAndSubmits(newPhone)
                    // Tampilkan error dalam dialog
                    errorText.text = getString(R.string.nomor_telepon_harus_minimal_10_digit_dan_format_valid)
                    errorText.visibility = View.VISIBLE
                    // Dialog tetap terbuka karena tidak ada dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun performPhoneUpdate(newPhone: String) {
        showLoading(true)
        UserManager.updatePhoneNumber(newPhone) { success ->
            activity?.runOnUiThread {
                showLoading(false)
                if (success) {
                    // BDD: THEN - Update berhasil
                    thenUserSeesSuccessMessageAndUpdatedProfile(newPhone)
                } else {
                    // BDD: THEN - Update gagal
                    thenUserSeesUpdateFailureAndReturnsToProfile()
                }
            }
        }
    }

    private fun loadUserProfile() {
        if (!UserManager.isUserLoggedIn()) {
            showError("User belum login")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            UserManager.getCurrentUserData { userData ->
                activity?.runOnUiThread {
                    showLoading(false)

                    if (userData != null) {
                        populateFields(userData)
                        Log.d(TAG, "Profile loaded for user: ${userData.email}")
                    } else {
                        showError("Gagal memuat data profil")
                    }
                }
            }
        }
    }

    private fun populateFields(userData: UserData) {
        with(binding) {
            namaLengkapLayout.editText?.setText(
                userData.namaLengkap.ifEmpty { "Tidak tersedia" }
            )

            pekerjaanLayout.editText?.setText(
                userData.pekerjaan.ifEmpty { "Tidak tersedia" }
            )

            nimLayout.editText?.setText(
                userData.nim.ifEmpty { "Tidak tersedia" }
            )

            programStudiLayout.editText?.setText(
                userData.programStudi.ifEmpty { "Tidak tersedia" }
            )

            nomorTeleponLayout.editText?.setText(
                userData.nomorTelepon.ifEmpty { "Tidak tersedia" }
            )
        }

        if (UserManager.isCurrentUserPredefined()) {
            showPredefinedUserUI()
        } else {
            showGenericUserUI(userData.email)
        }
    }

    private fun showPredefinedUserUI() {
        Log.d(TAG, "Showing predefined user UI")
    }

    private fun showGenericUserUI(email: String) {
        Log.d(TAG, "Showing generic user UI for: $email")

        Toast.makeText(
            requireContext(),
            "Profil terbatas untuk akun: $email",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setFieldsReadOnly() {
        with(binding) {
            namaLengkapLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            pekerjaanLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            nimLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            programStudiLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            // Nomor telepon tetap disabled karena kita pakai end icon untuk edit
            nomorTeleponLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
        }
    }

    private fun showLoading(show: Boolean) {
        with(binding) {
            val alpha = if (show) 0.5f else 1.0f
            namaLengkapLayout.alpha = alpha
            pekerjaanLayout.alpha = alpha
            nimLayout.alpha = alpha
            programStudiLayout.alpha = alpha
            nomorTeleponLayout.alpha = alpha
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Profile error: $message")
    }

    override fun onResume() {
        super.onResume()
        if (UserManager.isUserLoggedIn()) {
            loadUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}