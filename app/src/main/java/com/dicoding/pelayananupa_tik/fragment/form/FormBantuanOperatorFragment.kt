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
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormBantuanOperatorFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: FormData = FormData(),
        var isFormDataComplete: Boolean = false,
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class FormData(
        var jumlah: String = "",
        var kontak: String = "",
        var tujuan: String = "",
        var filePath: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_INCOMPLETE_DATA, FAILED_ERROR
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
        handlePdfPickerResult(result)
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

        // Ambil data dari form
        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.isFormDataComplete = validateCompleteFormData()

        if (scenarioContext.isFormDataComplete) {
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

        // Ambil data dari form
        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.isFormDataComplete = validateCompleteFormData()

        if (!scenarioContext.isFormDataComplete) {
            handleIncompleteFormSubmission()
        }
    }

    /**
     * THEN: Berhasil terkirim dan user melihat pesan konfirmasi (Skenario 1)
     */
    private fun thenFormSubmittedSuccessfullyWithConfirmation() {
        if (scenarioContext.isFormDataComplete) {
            scenarioContext.submitResult = SubmitResult.SUCCESS
            Log.d(TAG, "BDD - THEN: Form submitted successfully with confirmation message")

            // Proses penyimpanan ke Firestore
            executeSuccessfulFormSubmission()
        }
    }

    /**
     * THEN: Gagal terkirim dan user melihat pesan error dan kembali ke halaman formulir (Skenario 2)
     */
    private fun thenFormSubmissionFailsWithErrorMessage() {
        if (!scenarioContext.isFormDataComplete) {
            scenarioContext.submitResult = SubmitResult.FAILED_INCOMPLETE_DATA
            Log.d(TAG, "BDD - THEN: Form submission fails with error message and user stays at form page")

            showErrorMessageAndStayAtFormPage()
        }
    }

    /**
     * THEN: User mengalami error teknis saat submit
     */
    private fun thenUserExperiencesTechnicalError() {
        scenarioContext.submitResult = SubmitResult.FAILED_ERROR
        Log.d(TAG, "BDD - THEN: User experiences technical error during form submission")
        FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
    }

    // ==================== BDD HELPER METHODS ====================

    private fun getFormDataForBDD(): FormData {
        return FormData(
            jumlah = binding.jumlahLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            tujuan = binding.tujuanPeminjamanLayout.editText?.text.toString().trim(),
            filePath = savedPdfPath ?: ""
        )
    }

    private fun validateCompleteFormData(): Boolean {
        val data = scenarioContext.formData
        val isJumlahValid = data.jumlah.isNotEmpty()
        val isKontakValid = data.kontak.isNotEmpty() && isValidPhone(data.kontak)
        val isTujuanValid = data.tujuan.isNotEmpty()

        Log.d(TAG, "BDD - Form validation: jumlah=$isJumlahValid, kontak=$isKontakValid, tujuan=$isTujuanValid")

        return isJumlahValid && isKontakValid && isTujuanValid
    }

    private fun isValidPhone(phone: String): Boolean {
        // Implementasi validasi nomor telepon sederhana
        return phone.matches(Regex("^[+]?[0-9]{10,15}$"))
    }

    private fun processFormSubmission() {
        Log.d(TAG, "BDD - Processing form submission with complete data")
        thenFormSubmittedSuccessfullyWithConfirmation()
    }

    private fun handleIncompleteFormSubmission() {
        Log.d(TAG, "BDD - Handling incomplete form submission")
        thenFormSubmissionFailsWithErrorMessage()
    }

    private fun executeSuccessfulFormSubmission() {
        val dataToSave = mapOf(
            "judul" to "Bantuan Operator TIK",
            "jumlah" to scenarioContext.formData.jumlah,
            "kontak" to scenarioContext.formData.kontak,
            "tujuan" to scenarioContext.formData.tujuan,
            "filePath" to scenarioContext.formData.filePath
        )

        if (isEditMode) {
            executeUpdateForm(dataToSave)
        } else {
            executeNewFormSubmission(dataToSave)
        }
    }

    private fun executeNewFormSubmission(dataToSave: Map<String, String>) {
        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_bantuan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Form submitted successfully")
                showSuccessMessage("Formulir berhasil dikirim!")
                clearForm()
                findNavController().navigate(R.id.action_formBantuanOperatorFragment_to_historyLayananFragment)
            },
            onFailure = {
                Log.e(TAG, "BDD - ERROR: Form submission failed")
                thenUserExperiencesTechnicalError()
            }
        )
    }

    private fun executeUpdateForm(dataToSave: Map<String, String>) {
        editingItem?.documentId?.let { documentId ->
            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_bantuan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Form updated successfully")
                    showSuccessMessage("Formulir berhasil diperbarui!")
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formBantuanOperatorFragment_to_historyLayananFragment
                    )
                },
                onFailure = {
                    Log.e(TAG, "BDD - ERROR: Form update failed")
                    thenUserExperiencesTechnicalError()
                }
            )
        }
    }

    private fun showErrorMessageAndStayAtFormPage() {
        // Tampilkan pesan error spesifik berdasarkan field yang kosong
        val errorMessage = buildString {
            append("Harap lengkapi semua data yang wajib:\n")
            if (scenarioContext.formData.jumlah.isEmpty()) append("• Jumlah tidak boleh kosong\n")
            if (scenarioContext.formData.kontak.isEmpty()) append("• Kontak tidak boleh kosong\n")
            else if (!isValidPhone(scenarioContext.formData.kontak)) append("• Format kontak tidak valid\n")
            if (scenarioContext.formData.tujuan.isEmpty()) append("• Tujuan tidak boleh kosong\n")
        }.trimEnd()

        android.widget.Toast.makeText(
            requireContext(),
            errorMessage,
            android.widget.Toast.LENGTH_LONG
        ).show()

        // Highlight field yang error
        highlightErrorFields()

        // Reset button
        FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
    }

    private fun highlightErrorFields() {
        if (scenarioContext.formData.jumlah.isEmpty()) {
            binding.jumlahLayout.error = "Jumlah tidak boleh kosong"
        } else {
            binding.jumlahLayout.error = null
        }

        if (scenarioContext.formData.kontak.isEmpty()) {
            binding.kontakLayout.error = "Kontak tidak boleh kosong"
        } else if (!isValidPhone(scenarioContext.formData.kontak)) {
            binding.kontakLayout.error = "Format kontak tidak valid"
        } else {
            binding.kontakLayout.error = null
        }

        if (scenarioContext.formData.tujuan.isEmpty()) {
            binding.tujuanPeminjamanLayout.error = "Tujuan tidak boleh kosong"
        } else {
            binding.tujuanPeminjamanLayout.error = null
        }
    }

    private fun showSuccessMessage(message: String) {
        android.widget.Toast.makeText(
            requireContext(),
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // ==================== ORIGINAL IMPLEMENTATION METHODS ====================

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_bantuan_operator_tik)
    }

    private fun loadUserPhoneNumber() {
        FormUtils.loadUserPhoneNumber(
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
        // Update context dengan data terbaru
        scenarioContext.formData = getFormDataForBDD()
        scenarioContext.isFormDataComplete = validateCompleteFormData()

        if (scenarioContext.isFormDataComplete) {
            // BDD: WHEN - Skenario 1: User mengisi form lengkap
            whenUserFillsCompleteFormAndPressesSubmit()
        } else {
            // BDD: WHEN - Skenario 2: User mengisi form tidak lengkap
            whenUserFillsIncompleteFormAndPressesSubmit()
        }
    }

    private fun handlePdfPickerResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                if (isFileValid(requireContext(), uri)) {
                    updateFileSelection(uri)
                }
            }
        }
    }

    private fun updateFileSelection(uri: Uri) {
        val fileName = getFileName(requireContext(), uri)
        binding.tvFileName.text = getString(R.string.file_selected, " $fileName")

        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text = getString(R.string.change_image)
            strokeWidth = 0
        }

        savedPdfPath = savePdfLocally(requireContext(), uri)
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
                    binding.tvFileName.text = getString(R.string.file_selected, " ${file.name}")
                    binding.btnChooseFile.text = getString(R.string.change_file)
                    savedPdfPath = item.filePath
                }
            }
        }
    }

    private fun clearForm() {
        binding.jumlahLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPeminjamanLayout.editText?.text?.clear()
        resetFileSelection()

        // Clear BDD context
        scenarioContext.formData = FormData()
        scenarioContext.isFormDataComplete = false
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