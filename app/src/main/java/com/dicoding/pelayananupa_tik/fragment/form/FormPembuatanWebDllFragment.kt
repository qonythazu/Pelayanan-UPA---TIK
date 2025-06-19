package com.dicoding.pelayananupa_tik.fragment.form

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPembuatanWebDllBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleEditModeError
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleFormSubmission
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleUpdateNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.loadUserPhoneNumber
import com.dicoding.pelayananupa_tik.utils.FormUtils.resetButton
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.updateFormInFirestore
import com.google.firebase.firestore.FirebaseFirestore

class FormPembuatanWebDllFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PembuatanFormData = PembuatanFormData(),
        var validationResult: ValidationResult = ValidationResult(false),
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class PembuatanFormData(
        var layanan: String = "",
        var namaLayanan: String = "",
        var kontak: String = "",
        var tujuan: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_VALIDATION_ERROR, FAILED_TECHNICAL_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = FormScenarioContext()

    private var _binding: FragmentFormPembuatanWebDllBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPembuatanWebDllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        givenUserIsAtFormPage()
        checkEditMode()
        setupUI()
        loadUserPhoneNumber()
        setupClickListeners()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman formulir pembuatan email, website, subdomain, hosting dan/atau VPS
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form pembuatan web/dll page")
    }

    /**
     * WHEN: User mengisi semua data yang diminta di formulir dan menekan tombol submit (Skenario 1)
     */
    private fun whenUserFillsCompleteFormAndPressesSubmit() {
        if (!scenarioContext.userIsAtFormPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at form page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User fills complete form data and presses submit")

        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (scenarioContext.validationResult.isValid) {
            processFormSubmission()
        }
    }

    /**
     * WHEN: User mengisi formulir tanpa mengisi salah satu data yang wajib dan menekan tombol submit (Skenario 2)
     */
    private fun whenUserFillsIncompleteFormAndPressesSubmit() {
        if (!scenarioContext.userIsAtFormPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at form page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User fills incomplete form data and presses submit")

        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (!scenarioContext.validationResult.isValid) {
            handleIncompleteFormSubmission()
        }
    }

    /**
     * THEN: Berhasil terkirim dan user melihat pesan konfirmasi (Skenario 1)
     */
    private fun thenFormSubmittedSuccessfullyWithConfirmation() {
        if (scenarioContext.validationResult.isValid) {
            scenarioContext.submitResult = SubmitResult.SUCCESS
            Log.d(TAG, "BDD - THEN: Form submitted successfully with confirmation message")

            executeSuccessfulFormSubmission()
        }
    }

    /**
     * THEN: Gagal terkirim dan user melihat pesan error dan kembali ke halaman formulir (Skenario 2)
     */
    private fun thenFormSubmissionFailsWithValidationError() {
        if (!scenarioContext.validationResult.isValid) {
            scenarioContext.submitResult = SubmitResult.FAILED_VALIDATION_ERROR
            Log.d(TAG, "BDD - THEN: Form submission fails with validation error and user stays at form page")

            showValidationErrorsAndStayAtFormPage()
        }
    }

    /**
     * THEN: User mengalami error teknis saat submit
     */
    private fun thenUserExperiencesTechnicalError() {
        scenarioContext.submitResult = SubmitResult.FAILED_TECHNICAL_ERROR
        Log.d(TAG, "BDD - THEN: User experiences technical error during form submission")
        resetButton(binding.btnSubmit, if (isEditMode) R.string.update else R.string.submit, requireContext())
    }

    // ==================== BDD HELPER METHODS ====================

    private fun getFormDataForBDD(): PembuatanFormData {
        val selectedRadioButtonLayanan = binding.radioGroupServices.checkedRadioButtonId

        val layanan = when {
            selectedRadioButtonLayanan == R.id.radioOther -> binding.editTextOther.text.toString().trim()
            selectedRadioButtonLayanan != -1 -> view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
            else -> ""
        }

        return PembuatanFormData(
            layanan = layanan,
            namaLayanan = binding.namaLayananLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            tujuan = binding.tujuanPembuatanLayout.editText?.text.toString().trim()
        )
    }

    private fun validateFormUsingHelper(): ValidationResult {
        val validationRules = buildValidation {
            required(
                field = scenarioContext.formData.namaLayanan,
                layout = binding.namaLayananLayout,
                errorMessage = "Nama Layanan tidak boleh kosong"
            )

            required(
                field = scenarioContext.formData.tujuan,
                layout = binding.tujuanPembuatanLayout,
                errorMessage = "Tujuan tidak boleh kosong"
            )

            phone(
                field = scenarioContext.formData.kontak,
                layout = binding.kontakLayout,
                errorMessage = "Format kontak tidak valid"
            )

            radioButton(
                field = scenarioContext.formData.layanan,
                errorMessage = "Harap pilih layanan"
            )
        }

        val result = ValidationHelper.validateFormWithRules(requireContext(), validationRules)

        Log.d(TAG, "BDD - Form validation result: isValid=${result.isValid}, errors=${result.errors}")
        return result
    }

    private fun processFormSubmission() {
        Log.d(TAG, "BDD - Processing form submission with complete data")
        thenFormSubmittedSuccessfullyWithConfirmation()
    }

    private fun handleIncompleteFormSubmission() {
        Log.d(TAG, "BDD - Handling incomplete form submission")
        thenFormSubmissionFailsWithValidationError()
    }

    private fun executeSuccessfulFormSubmission() {
        val dataToSave = mapOf(
            "judul" to "Form Pembuatan Web/DLL",
            "layanan" to scenarioContext.formData.layanan,
            "namaLayanan" to scenarioContext.formData.namaLayanan,
            "kontak" to scenarioContext.formData.kontak,
            "tujuan" to scenarioContext.formData.tujuan
        )

        handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = dataToSave,
            validationResult = scenarioContext.validationResult.isValid,
            onSubmit = { executeNewFormSubmission(dataToSave) },
            onUpdate = { executeUpdateForm(dataToSave) }
        )
    }

    private fun executeNewFormSubmission(dataToSave: Map<String, Any>) {
        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pembuatan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Form submitted successfully")
                android.widget.Toast.makeText(requireContext(), "Formulir berhasil dikirim!", android.widget.Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment)
            },
            onFailure = {
                Log.e(TAG, "BDD - ERROR: Form submission failed")
                thenUserExperiencesTechnicalError()
            }
        )
    }

    private fun executeUpdateForm(dataToSave: Map<String, Any>) {
        handleEditModeError(
            editingItem = editingItem,
            submitButton = binding.btnSubmit,
            context = requireContext()
        ) { documentId ->
            updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pembuatan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Form updated successfully")
                    android.widget.Toast.makeText(requireContext(), "Formulir berhasil diperbarui!", android.widget.Toast.LENGTH_SHORT).show()
                    resetButton(binding.btnSubmit, R.string.update, requireContext())
                    handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment
                    )
                },
                onFailure = { errorMsg ->
                    Log.e(TAG, "BDD - ERROR: Form update failed: $errorMsg")
                    thenUserExperiencesTechnicalError()
                }
            )
        }
    }

    private fun showValidationErrorsAndStayAtFormPage() {
        if (scenarioContext.validationResult.errors.isNotEmpty()) {
            val errorMessage = scenarioContext.validationResult.errors.joinToString("\n")
            android.widget.Toast.makeText(
                requireContext(),
                errorMessage,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        resetButton(
            binding.btnSubmit,
            if (isEditMode) R.string.update else R.string.submit,
            requireContext()
        )
    }

    // ==================== ORIGINAL IMPLEMENTATION METHODS ====================

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pembuatan_web_dll)
        setupRadioGroupListener()
    }

    private fun setupRadioGroupListener() {
        binding.radioGroupServices.setOnCheckedChangeListener { _, checkedId ->
            clearRadioGroupErrorState(binding.radioGroupServices)

            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun clearRadioGroupErrorState(radioGroup: RadioGroup) {
        radioGroup.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    private fun loadUserPhoneNumber() {
        loadUserPhoneNumber(
            firestore = firestore,
            isEditMode = isEditMode,
            currentContactText = binding.kontakLayout.editText?.text.toString()
        ) { phoneNumber ->
            binding.kontakLayout.editText?.setText(phoneNumber)
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            // BDD: WHEN - User menekan tombol submit
            handleFormSubmissionWithBDD()
        }
    }

    private fun handleFormSubmissionWithBDD() {
        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (scenarioContext.validationResult.isValid) {
            // BDD: WHEN - Skenario 1: User mengisi form lengkap
            whenUserFillsCompleteFormAndPressesSubmit()
        } else {
            // BDD: WHEN - Skenario 2: User mengisi form tidak lengkap
            whenUserFillsIncompleteFormAndPressesSubmit()
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    layanan = args.getString("layanan") ?: "",
                    namaLayanan = args.getString("namaLayanan") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            setupRadioButtonSelection(item.layanan)
            binding.namaLayananLayout.editText?.setText(item.namaLayanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPembuatanLayout.editText?.setText(item.tujuan)
        }
    }

    private fun setupRadioButtonSelection(layanan: String) {
        val layananRadioButtons = mapOf(
            "Subdomain" to binding.radioSubDomain,
            "Hosting" to binding.radioHosting,
            "Virtual Private Server (VPS)" to binding.radioVPS,
            "Website" to binding.radioWebsite,
            "Email" to binding.radioEmail
        )

        if (layananRadioButtons.containsKey(layanan)) {
            layananRadioButtons[layanan]?.isChecked = true
        } else {
            binding.radioOther.isChecked = true
            binding.textInputLayoutOther.visibility = View.VISIBLE
            binding.editTextOther.setText(layanan)
        }
    }

    private fun clearForm() {
        binding.radioGroupServices.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaLayananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPembuatanLayout.editText?.text?.clear()
        binding.namaLayananLayout.error = null
        binding.kontakLayout.error = null
        binding.tujuanPembuatanLayout.error = null
        clearRadioGroupErrorState(binding.radioGroupServices)

        scenarioContext.formData = PembuatanFormData()
        scenarioContext.validationResult = ValidationResult(false)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNavigation()
        (activity as? MainActivity)?.showToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "FormPembuatanWebDllFragment"
    }
}