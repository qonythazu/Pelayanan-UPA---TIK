package com.dicoding.pelayananupa_tik.fragment.form

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormBantuanOperatorBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleEditModeError
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleFormSubmission
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleUpdateNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.loadUserPhoneNumber
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.resetButton
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.updateFormInFirestore
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormBantuanOperatorFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: BantuanFormData = BantuanFormData(),
        var validationResult: ValidationResult = ValidationResult(false),
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class BantuanFormData(
        var jumlah: String = "",
        var kontak: String = "",
        var tujuan: String = "",
        var filePath: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_VALIDATION_ERROR, FAILED_TECHNICAL_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = FormScenarioContext()

    private var _binding: FragmentFormBantuanOperatorBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var selectedPdfUri: Uri? = null
    private var savedPdfPath: String? = null
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handlePdfSelection(result.data?.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormBantuanOperatorBinding.inflate(inflater, container, false)
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
     * GIVEN: User telah login dan berada di halaman formulir bantuan operator TIK
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form bantuan operator TIK page")
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

    private fun getFormDataForBDD(): BantuanFormData {
        return BantuanFormData(
            jumlah = binding.jumlahLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            tujuan = binding.tujuanPeminjamanLayout.editText?.text.toString().trim(),
            filePath = savedPdfPath ?: ""
        )
    }

    private fun validateFormUsingHelper(): ValidationResult {
        val validationRules = buildValidation {
            required(
                field = scenarioContext.formData.jumlah,
                layout = binding.jumlahLayout,
                errorMessage = "Jumlah tidak boleh kosong"
            )

            required(
                field = scenarioContext.formData.tujuan,
                layout = binding.tujuanPeminjamanLayout,
                errorMessage = "Tujuan tidak boleh kosong"
            )

            phone(
                field = scenarioContext.formData.kontak,
                layout = binding.kontakLayout,
                errorMessage = "Format nomor telepon tidak valid"
            )

            // Validate file selection (optional for this form, remove if not required)
            // file(
            //     uri = selectedPdfUri,
            //     context = requireContext(),
            //     errorMessage = "Harap pilih file"
            // )
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
            "judul" to "Bantuan Operator TIK",
            "jumlah" to scenarioContext.formData.jumlah,
            "kontak" to scenarioContext.formData.kontak,
            "tujuan" to scenarioContext.formData.tujuan,
            "filePath" to scenarioContext.formData.filePath
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
            collectionName = "form_bantuan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Form submitted successfully")
                clearForm()
                findNavController().navigate(R.id.action_formBantuanOperatorFragment_to_historyLayananFragment)
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
                collectionName = "form_bantuan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Form updated successfully")
                    resetButton(binding.btnSubmit, R.string.update, requireContext())
                    handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formBantuanOperatorFragment_to_historyLayananFragment
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_bantuan_operator_tik)
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
        binding.btnChooseFile.setOnClickListener {
            openPdfPicker(pdfPickerLauncher)
        }

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

    private fun handlePdfSelection(uri: Uri?) {
        selectedPdfUri = uri
        uri?.let {
            if (isFileValid(requireContext(), it)) {
                val fileName = getFileName(requireContext(), it)
                updateFileSelection(fileName)
                savedPdfPath = savePdfLocally(requireContext(), it)
            }
        }
    }

    private fun updateFileSelection(fileName: String) {
        binding.tvFileName.text = getString(R.string.file_selected, " $fileName")

        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text = getString(R.string.change_image)
            strokeWidth = 0
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    jumlah = args.getString("jumlah") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.jumlahLayout.editText?.setText(item.jumlah)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPeminjamanLayout.editText?.setText(item.tujuan)

            if (item.filePath.isNotEmpty()) {
                val file = File(item.filePath)
                if (file.exists()) {
                    updateFileSelection(file.name)
                    savedPdfPath = item.filePath
                    selectedPdfUri = Uri.fromFile(file)
                }
            }
        }
    }

    private fun clearForm() {
        binding.jumlahLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPeminjamanLayout.editText?.text?.clear()
        binding.jumlahLayout.error = null
        binding.kontakLayout.error = null
        binding.tujuanPeminjamanLayout.error = null

        resetFileSelection()

        scenarioContext.formData = BantuanFormData()
        scenarioContext.validationResult = ValidationResult(false)
    }

    private fun resetFileSelection() {
        binding.tvFileName.text = getString(R.string.no_file_selected)
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
        }
        selectedPdfUri = null
        savedPdfPath = null
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
        private const val TAG = "FormBantuanOperatorFragment"
    }
}