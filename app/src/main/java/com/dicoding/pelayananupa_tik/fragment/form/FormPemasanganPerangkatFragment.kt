package com.dicoding.pelayananupa_tik.fragment.form

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemasanganPerangkatBinding
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

class FormPemasanganPerangkatFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PemasanganFormData = PemasanganFormData(),
        var validationResult: ValidationResult = ValidationResult(false),
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class PemasanganFormData(
        var jenis: String = "",
        var kontak: String = "",
        var tujuan: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_VALIDATION_ERROR, FAILED_TECHNICAL_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = FormScenarioContext()

    private var _binding: FragmentFormPemasanganPerangkatBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPemasanganPerangkatBinding.inflate(inflater, container, false)
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
     * GIVEN: User telah login dan berada di halaman formulir pemasangan perangkat TIK
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form pemasangan perangkat TIK page")
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

    private fun getFormDataForBDD(): PemasanganFormData {
        return PemasanganFormData(
            jenis = binding.jenisPerangkatLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            tujuan = binding.tujuanPemasanganLayout.editText?.text.toString().trim()
        )
    }

    private fun validateFormUsingHelper(): ValidationResult {
        val validationRules = buildValidation {
            required(
                field = scenarioContext.formData.jenis,
                layout = binding.jenisPerangkatLayout,
                errorMessage = "Jenis perangkat tidak boleh kosong"
            )

            required(
                field = scenarioContext.formData.tujuan,
                layout = binding.tujuanPemasanganLayout,
                errorMessage = "Tujuan pemasangan tidak boleh kosong"
            )

            phone(
                field = scenarioContext.formData.kontak,
                layout = binding.kontakLayout,
                errorMessage = "Format nomor telepon tidak valid"
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
            "judul" to "Pemasangan Perangkat",
            "jenis" to scenarioContext.formData.jenis,
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
            collectionName = "form_pemasangan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Form submitted successfully")
                android.widget.Toast.makeText(requireContext(), "Formulir berhasil dikirim!", android.widget.Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPemasanganPerangkatFragment_to_historyLayananFragment)
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
            val updateData = mapOf(
                "jenis" to scenarioContext.formData.jenis,
                "kontak" to scenarioContext.formData.kontak,
                "tujuan" to scenarioContext.formData.tujuan
            )

            updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pemasangan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Form updated successfully")
                    android.widget.Toast.makeText(requireContext(), "Formulir berhasil diperbarui!", android.widget.Toast.LENGTH_SHORT).show()
                    resetButton(binding.btnSubmit, R.string.update, requireContext())
                    handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPemasanganPerangkatFragment_to_historyLayananFragment
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pemasangan_perangkat)
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
                    jenis = args.getString("jenis") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.jenisPerangkatLayout.editText?.setText(item.jenis)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPemasanganLayout.editText?.setText(item.tujuan)
        }
    }

    private fun clearForm() {
        binding.jenisPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPemasanganLayout.editText?.text?.clear()
        binding.jenisPerangkatLayout.error = null
        binding.kontakLayout.error = null
        binding.tujuanPemasanganLayout.error = null

        scenarioContext.formData = PemasanganFormData()
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
        private const val TAG = "FormPemasanganPerangkatFragment"
    }
}