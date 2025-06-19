package com.dicoding.pelayananupa_tik.fragment.form

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemeliharaanAkunBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleEditModeError
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleFormSubmission
import com.dicoding.pelayananupa_tik.utils.FormUtils.handleUpdateNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.resetButton
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.dicoding.pelayananupa_tik.utils.FormUtils.updateFormInFirestore
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormPemeliharaanAkunFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PemeliharaanFormData = PemeliharaanFormData(),
        var validationResult: ValidationResult = ValidationResult(false),
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class PemeliharaanFormData(
        var layanan: String = "",
        var jenis: String = "",
        var akun: String = "",
        var alasan: String = "",
        var filePath: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_VALIDATION_ERROR, FAILED_TECHNICAL_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = FormScenarioContext()

    private var _binding: FragmentFormPemeliharaanAkunBinding? = null
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
        _binding = FragmentFormPemeliharaanAkunBinding.inflate(
            inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        givenUserIsAtFormPage()
        checkEditMode()
        setupUI()
        setupClickListeners()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman formulir pemeliharaan akun layanan
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form pemeliharaan akun layanan page")
    }

    /**
     * WHEN: User mengisi semua data yang diminta di formulir
     * dan menekan tombol submit (Skenario 1)
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
     * WHEN: User mengisi formulir tanpa mengisi salah satu data yang wajib
     * dan menekan tombol submit (Skenario 2)
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

    private fun getFormDataForBDD(): PemeliharaanFormData {
        return PemeliharaanFormData(
            layanan = getSelectedRadioButtonText(binding.radioGroupLayanan),
            jenis = getJenisValue(),
            akun = binding.namaAkunLayout.editText?.text.toString().trim(),
            alasan = binding.alasanLayout.editText?.text.toString().trim(),
            filePath = savedPdfPath ?: ""
        )
    }

    private fun validateFormUsingHelper(): ValidationResult {
        val validationRules = buildValidation {
            required(
                field = scenarioContext.formData.akun,
                layout = binding.namaAkunLayout,
                errorMessage = "Nama akun layanan tidak boleh kosong"
            )

            required(
                field = scenarioContext.formData.alasan,
                layout = binding.alasanLayout,
                errorMessage = "Alasan pemeliharaan tidak boleh kosong"
            )

            radioButton(
                field = scenarioContext.formData.layanan,
                errorMessage = "Harap pilih layanan"
            )

            radioButton(
                field = scenarioContext.formData.jenis,
                errorMessage = "Harap pilih jenis pemeliharaan"
            )

            file(
                uri = selectedPdfUri,
                context = requireContext(),
                errorMessage = "Harap pilih file"
            )
        }

        val result = ValidationHelper.validateFormWithRules(requireContext(), validationRules)
        Log.d(
            TAG,
            "BDD - Form validation result: isValid=${result.isValid}," +
                    " errors=${result.errors}")

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
            "judul" to "Form Pemeliharaan Akun",
            "layanan" to scenarioContext.formData.layanan,
            "jenis" to scenarioContext.formData.jenis,
            "akun" to scenarioContext.formData.akun,
            "alasan" to scenarioContext.formData.alasan,
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
            collectionName = "form_pemeliharaan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Pemeliharaan form submitted successfully")
                clearForm()
                findNavController().navigate(R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment)
            },
            onFailure = {
                Log.e(TAG, "BDD - ERROR: Pemeliharaan form submission failed")
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
                collectionName = "form_pemeliharaan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Pemeliharaan form updated successfully")
                    resetButton(binding.btnSubmit, R.string.update, requireContext())
                    handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment
                    )
                },
                onFailure = { errorMsg ->
                    Log.e(TAG, "BDD - ERROR: Pemeliharaan form update failed: $errorMsg")
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pemeliharaan_akun)
        setupRadioGroupListeners()
    }

    private fun setupRadioGroupListeners() {
        binding.radioGroupLayanan.setOnCheckedChangeListener { _, _ ->
            clearRadioGroupErrorState(binding.radioGroupLayanan)
        }
        binding.radioGroupJenis.setOnCheckedChangeListener { _, checkedId ->
            clearRadioGroupErrorState(binding.radioGroupJenis)
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

    private fun setupClickListeners() {
        binding.btnChooseFile.setOnClickListener { openPdfPicker(pdfPickerLauncher) }
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
                updateFileUI(fileName)
                savedPdfPath = savePdfLocally(requireContext(), it)
                binding.tvFileName.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.black)
                )
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
                    jenis = args.getString("jenis") ?: "",
                    akun = args.getString("akun") ?: "",
                    alasan = args.getString("alasan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            populateLayananRadioButtons(item.layanan)
            populateJenisRadioButtons(item.jenis)
            binding.namaAkunLayout.editText?.setText(item.akun)
            binding.alasanLayout.editText?.setText(item.alasan)
            populateFileData(item.filePath)
        }
    }

    private fun populateLayananRadioButtons(layanan: String) {
        val layananRadioButtons = mapOf(
            "Subdomain" to binding.radioSubDomain,
            "Hosting" to binding.radioHosting,
            "Virtual Private Server (VPS)" to binding.radioVPS,
            "Website" to binding.radioWebsite,
            "Email" to binding.radioEmail,
            "Gerbang ITK" to binding.radioGerbang,
            "Microsoft 365" to binding.radioMic
        )
        layananRadioButtons[layanan]?.isChecked = true
    }

    private fun populateJenisRadioButtons(jenis: String) {
        val jenisRadioButtons = mapOf(
            "Reset Password Akun" to binding.radioReset,
            "Perubahan/Penambahan Data Layanan" to binding.radioPerubahan,
            "Penambahan Penyimpanan" to binding.radioPenambahan
        )

        if (jenisRadioButtons.containsKey(jenis)) {
            jenisRadioButtons[jenis]?.isChecked = true
        } else {
            binding.radioOther.isChecked = true
            binding.textInputLayoutOther.visibility = View.VISIBLE
            binding.editTextOther.setText(jenis)
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

    private fun getSelectedRadioButtonText(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            view?.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        } else ""
    }

    private fun getJenisValue(): String {
        return when (val selectedId = binding.radioGroupJenis.checkedRadioButtonId) {
            R.id.radioOther -> binding.editTextOther.text.toString().trim()
            -1 -> ""
            else -> view?.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        }
    }

    private fun clearForm() {
        binding.radioGroupLayanan.clearCheck()
        binding.radioGroupJenis.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaAkunLayout.editText?.text?.clear()
        binding.alasanLayout.editText?.text?.clear()
        binding.namaAkunLayout.error = null
        binding.alasanLayout.error = null
        clearRadioGroupErrorState(binding.radioGroupLayanan)
        clearRadioGroupErrorState(binding.radioGroupJenis)

        resetFileUI()
        selectedPdfUri = null
        savedPdfPath = null

        scenarioContext.formData = PemeliharaanFormData()
        scenarioContext.validationResult = ValidationResult(false)
    }

    private fun resetFileUI() {
        binding.tvFileName.text = getString(R.string.no_file_selected)
        binding.tvFileName.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
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
        private const val TAG = "FormPemeliharaanAkunFragment"
    }
}