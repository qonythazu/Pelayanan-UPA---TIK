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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPengaduanLayananBinding
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

class FormPengaduanLayananFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class PengaduanScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PengaduanFormData = PengaduanFormData(),
        var validationResult: ValidationResult = ValidationResult(false),
        var submitResult: PengaduanSubmitResult = PengaduanSubmitResult.PENDING
    )

    private data class PengaduanFormData(
        var layanan: String = "",
        var kontak: String = "",
        var keluhan: String = "",
        var filePath: String = ""
    )

    private enum class PengaduanSubmitResult {
        PENDING, SUCCESS, FAILED_VALIDATION_ERROR, FAILED_TECHNICAL_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = PengaduanScenarioContext()

    private var _binding: FragmentFormPengaduanLayananBinding? = null
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
        _binding = FragmentFormPengaduanLayananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        givenUserIsAtPengaduanFormPage()
        checkEditMode()
        setupUI()
        loadUserPhoneNumber()
        setupClickListeners()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman formulir layanan pengaduan
     */
    private fun givenUserIsAtPengaduanFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = PengaduanSubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form pengaduan layanan page")
    }

    /**
     * WHEN: User mengisi semua data yang diminta di formulir dan menekan tombol submit (Skenario 1)
     */
    private fun whenUserFillsCompletePengaduanFormAndPressesSubmit() {
        if (!scenarioContext.userIsAtFormPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at pengaduan form page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User fills complete pengaduan form data and presses submit")

        scenarioContext.formData = getPengaduanFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (scenarioContext.validationResult.isValid) {
            processPengaduanFormSubmission()
        }
    }

    /**
     * WHEN: User mengisi formulir tanpa mengisi salah satu data yang wajib dan menekan tombol submit (Skenario 2)
     */
    private fun whenUserFillsIncompletePengaduanFormAndPressesSubmit() {
        if (!scenarioContext.userIsAtFormPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at pengaduan form page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User fills incomplete pengaduan form data and presses submit")

        scenarioContext.formData = getPengaduanFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (!scenarioContext.validationResult.isValid) {
            handleIncompletePengaduanFormSubmission()
        }
    }

    /**
     * THEN: Berhasil terkirim dan user melihat pesan konfirmasi (Skenario 1)
     */
    private fun thenPengaduanFormSubmittedSuccessfullyWithConfirmation() {
        if (scenarioContext.validationResult.isValid) {
            scenarioContext.submitResult = PengaduanSubmitResult.SUCCESS
            Log.d(TAG, "BDD - THEN: Pengaduan form submitted successfully with confirmation message")

            executeSuccessfulPengaduanFormSubmission()
        }
    }

    /**
     * THEN: Gagal terkirim dan user melihat pesan error "Harap lengkapi semua data yang wajib"
     * dan user kembali ke halaman formulir layanan pengaduan (Skenario 2)
     */
    private fun thenPengaduanFormSubmissionFailsWithErrorMessage() {
        if (!scenarioContext.validationResult.isValid) {
            scenarioContext.submitResult = PengaduanSubmitResult.FAILED_VALIDATION_ERROR
            Log.d(TAG, "BDD - THEN: Pengaduan form submission fails with error message and user stays at form page")

            showValidationErrorsAndStayAtFormPage()
        }
    }

    /**
     * THEN: User mengalami error teknis saat submit pengaduan
     */
    private fun thenUserExperiencesPengaduanTechnicalError() {
        scenarioContext.submitResult = PengaduanSubmitResult.FAILED_TECHNICAL_ERROR
        Log.d(TAG, "BDD - THEN: User experiences technical error during pengaduan form submission")
        resetButton(binding.btnSubmit, if (isEditMode) R.string.update else R.string.submit, requireContext())
    }

    // ==================== BDD HELPER METHODS ====================

    private fun getPengaduanFormDataForBDD(): PengaduanFormData {
        return PengaduanFormData(
            layanan = binding.layananLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            keluhan = binding.keluhanAndaLayout.editText?.text.toString().trim(),
            filePath = savedPdfPath ?: ""
        )
    }

    private fun validateFormUsingHelper(): ValidationResult {
        val validationRules = buildValidation {
            required(
                field = scenarioContext.formData.layanan,
                layout = binding.layananLayout,
                errorMessage = "Layanan tidak boleh kosong"
            )

            required(
                field = scenarioContext.formData.keluhan,
                layout = binding.keluhanAndaLayout,
                errorMessage = "Keluhan tidak boleh kosong"
            )

            phone(
                field = scenarioContext.formData.kontak,
                layout = binding.kontakLayout,
                errorMessage = "Format kontak tidak valid"
            )

            // Validate file selection (optional untuk pengaduan)
            // Uncomment jika file wajib diisi
            // file(
            //     uri = selectedPdfUri,
            //     context = requireContext(),
            //     errorMessage = "Harap pilih file"
            // )
        }

        val result = ValidationHelper.validateFormWithRules(requireContext(), validationRules)

        Log.d(TAG, "BDD - Pengaduan form validation result: isValid=${result.isValid}, errors=${result.errors}")
        return result
    }

    private fun processPengaduanFormSubmission() {
        Log.d(TAG, "BDD - Processing pengaduan form submission with complete data")
        thenPengaduanFormSubmittedSuccessfullyWithConfirmation()
    }

    private fun handleIncompletePengaduanFormSubmission() {
        Log.d(TAG, "BDD - Handling incomplete pengaduan form submission")
        thenPengaduanFormSubmissionFailsWithErrorMessage()
    }

    private fun executeSuccessfulPengaduanFormSubmission() {
        val dataToSave = mapOf(
            "judul" to "Form Pengaduan Layanan",
            "layanan" to scenarioContext.formData.layanan,
            "kontak" to scenarioContext.formData.kontak,
            "keluhan" to scenarioContext.formData.keluhan,
            "filePath" to scenarioContext.formData.filePath
        )

        handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = dataToSave,
            validationResult = scenarioContext.validationResult.isValid,
            onSubmit = { executeNewPengaduanFormSubmission(dataToSave) },
            onUpdate = { executeUpdatePengaduanForm(dataToSave) }
        )
    }

    private fun executeNewPengaduanFormSubmission(dataToSave: Map<String, Any>) {
        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pengaduan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Pengaduan form submitted successfully")
                clearForm()
                findNavController().navigate(R.id.action_formPengaduanLayananFragment_to_historyLayananFragment)
            },
            onFailure = {
                Log.e(TAG, "BDD - ERROR: Pengaduan form submission failed")
                thenUserExperiencesPengaduanTechnicalError()
            }
        )
    }

    private fun executeUpdatePengaduanForm(dataToSave: Map<String, Any>) {
        handleEditModeError(
            editingItem = editingItem,
            submitButton = binding.btnSubmit,
            context = requireContext()
        ) { documentId ->
            updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pengaduan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Pengaduan form updated successfully")
                    resetButton(binding.btnSubmit, R.string.update, requireContext())
                    handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPengaduanLayananFragment_to_historyLayananFragment
                    )
                },
                onFailure = { errorMsg ->
                    Log.e(TAG, "BDD - ERROR: Pengaduan form update failed: $errorMsg")
                    thenUserExperiencesPengaduanTechnicalError()
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pengaduan_layanan)
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
            handlePengaduanFormSubmissionWithBDD()
        }
    }

    private fun handlePengaduanFormSubmissionWithBDD() {
        scenarioContext.formData = getPengaduanFormDataForBDD()
        scenarioContext.validationResult = validateFormUsingHelper()

        if (scenarioContext.validationResult.isValid) {
            // BDD: WHEN - Skenario 1: User mengisi form pengaduan lengkap
            whenUserFillsCompletePengaduanFormAndPressesSubmit()
        } else {
            // BDD: WHEN - Skenario 2: User mengisi form pengaduan tidak lengkap
            whenUserFillsIncompletePengaduanFormAndPressesSubmit()
        }
    }

    private fun handlePdfSelection(uri: Uri?) {
        selectedPdfUri = uri
        uri?.let {
            if (isFileValid(requireContext(), it)) {
                val fileName = getFileName(requireContext(), it)
                updateFileUI(fileName)
                savedPdfPath = savePdfLocally(requireContext(), it)
            }
        }
    }

    private fun updateFileUI(fileName: String) {
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
                    layanan = args.getString("layanan") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    keluhan = args.getString("keluhan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.layananLayout.editText?.setText(item.layanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.keluhanAndaLayout.editText?.setText(item.keluhan)
            populateFileData(item.filePath)
        }
    }

    private fun populateFileData(filePath: String) {
        if (filePath.isNotEmpty()) {
            val file = File(filePath)
            if (file.exists()) {
                updateFileUI(file.name)
                savedPdfPath = filePath
                selectedPdfUri = Uri.fromFile(file)
            }
        }
    }

    private fun clearForm() {
        binding.layananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keluhanAndaLayout.editText?.text?.clear()
        binding.layananLayout.error = null
        binding.kontakLayout.error = null
        binding.keluhanAndaLayout.error = null

        resetFileUI()
        selectedPdfUri = null
        savedPdfPath = null

        scenarioContext.formData = PengaduanFormData()
        scenarioContext.validationResult = ValidationResult(false)
    }

    private fun resetFileUI() {
        binding.tvFileName.text = ""
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
        }
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
        private const val TAG = "FormPengaduanLayananFragment"
    }
}