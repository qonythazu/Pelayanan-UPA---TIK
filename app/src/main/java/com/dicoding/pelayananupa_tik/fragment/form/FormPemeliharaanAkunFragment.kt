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
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormPemeliharaanAkunFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PemeliharaanFormData = PemeliharaanFormData(),
        var isFormDataComplete: Boolean = false,
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
        PENDING, SUCCESS, FAILED_INCOMPLETE_DATA, FAILED_ERROR
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
        _binding = FragmentFormPemeliharaanAkunBinding.inflate(inflater, container, false)
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

    private fun getFormDataForBDD(): PemeliharaanFormData {
        return PemeliharaanFormData(
            layanan = getSelectedRadioButtonText(binding.radioGroupLayanan),
            jenis = getJenisValue(),
            akun = binding.namaAkunLayout.editText?.text.toString().trim(),
            alasan = binding.alasanLayout.editText?.text.toString().trim(),
            filePath = savedPdfPath ?: ""
        )
    }

    private fun validateCompleteFormData(): Boolean {
        val data = scenarioContext.formData
        val isLayananValid = data.layanan.isNotEmpty()
        val isJenisValid = data.jenis.isNotEmpty()
        val isAkunValid = data.akun.isNotEmpty()
        val isAlasanValid = data.alasan.isNotEmpty()
        val isFileValid = selectedPdfUri != null

        Log.d(TAG, "BDD - Form validation: layanan=$isLayananValid, jenis=$isJenisValid, akun=$isAkunValid, alasan=$isAlasanValid, file=$isFileValid")

        return isLayananValid && isJenisValid && isAkunValid && isAlasanValid && isFileValid
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
            "judul" to "Form Pemeliharaan Akun",
            "layanan" to scenarioContext.formData.layanan,
            "jenis" to scenarioContext.formData.jenis,
            "akun" to scenarioContext.formData.akun,
            "alasan" to scenarioContext.formData.alasan,
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
            collectionName = "form_pemeliharaan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Pemeliharaan form submitted successfully")
                showSuccessMessage("Formulir pemeliharaan akun berhasil dikirim!")
                clearForm()
                findNavController().navigate(R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment)
            },
            onFailure = {
                Log.e(TAG, "BDD - ERROR: Pemeliharaan form submission failed")
                thenUserExperiencesTechnicalError()
            }
        )
    }

    private fun executeUpdateForm(dataToSave: Map<String, String>) {
        editingItem?.documentId?.let { documentId ->
            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pemeliharaan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Pemeliharaan form updated successfully")
                    showSuccessMessage("Formulir pemeliharaan akun berhasil diperbarui!")
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment
                    )
                },
                onFailure = {
                    Log.e(TAG, "BDD - ERROR: Pemeliharaan form update failed")
                    thenUserExperiencesTechnicalError()
                }
            )
        }
    }

    private fun showErrorMessageAndStayAtFormPage() {
        // Tampilkan pesan error spesifik berdasarkan field yang kosong
        val errorMessage = buildString {
            append("Harap lengkapi semua data yang wajib:\n")
            if (scenarioContext.formData.layanan.isEmpty()) append("• Layanan harus dipilih\n")
            if (scenarioContext.formData.jenis.isEmpty()) append("• Jenis pemeliharaan harus dipilih\n")
            if (scenarioContext.formData.akun.isEmpty()) append("• Nama akun layanan tidak boleh kosong\n")
            if (scenarioContext.formData.alasan.isEmpty()) append("• Alasan pemeliharaan tidak boleh kosong\n")
            if (selectedPdfUri == null) append("• File harus dipilih\n")
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
        // Highlight radio button errors by changing background color temporarily
        if (scenarioContext.formData.layanan.isEmpty()) {
            // Visual feedback for radio group layanan
            binding.radioGroupLayanan.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.red)
            )
        } else {
            binding.radioGroupLayanan.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        if (scenarioContext.formData.jenis.isEmpty()) {
            // Visual feedback for radio group jenis
            binding.radioGroupJenis.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.red)
            )
        } else {
            binding.radioGroupJenis.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        if (scenarioContext.formData.akun.isEmpty()) {
            binding.namaAkunLayout.error = "Nama akun layanan tidak boleh kosong"
        } else {
            binding.namaAkunLayout.error = null
        }

        if (scenarioContext.formData.alasan.isEmpty()) {
            binding.alasanLayout.error = "Alasan pemeliharaan tidak boleh kosong"
        } else {
            binding.alasanLayout.error = null
        }

        if (selectedPdfUri == null) {
            binding.tvFileName.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.red)
            )
        } else {
            binding.tvFileName.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pemeliharaan_akun)
        setupRadioGroupListeners()
    }

    private fun setupRadioGroupListeners() {
        binding.radioGroupLayanan.setOnCheckedChangeListener { _, _ -> }
        binding.radioGroupJenis.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnChooseFile.setOnClickListener { openPdfPicker(pdfPickerLauncher) }
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
        resetFileUI()
        selectedPdfUri = null
        savedPdfPath = null

        // Clear BDD context
        scenarioContext.formData = PemeliharaanFormData()
        scenarioContext.isFormDataComplete = false
    }

    private fun resetFileUI() {
        binding.tvFileName.text = getString(R.string.no_file_selected)
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