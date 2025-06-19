package com.dicoding.pelayananupa_tik.fragment.peminjamanFeature

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel
import com.dicoding.pelayananupa_tik.backend.model.Barang
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPeminjamanBinding
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FormPeminjamanFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class PeminjamanScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: PeminjamanFormData = PeminjamanFormData("", "", "", "", ""),
        var isFormDataComplete: Boolean = false,
        var submitResult: SubmitResult = SubmitResult.PENDING,
        var validationErrors: List<String> = emptyList()
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_INCOMPLETE_DATA, FAILED_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = PeminjamanScenarioContext()

    private var _binding: FragmentFormPeminjamanBinding? = null
    private val binding get() = _binding!!
    private var selectedItems: String? = null
    private lateinit var firestore: FirebaseFirestore
    private val boxViewModel: BoxViewModel by activityViewModels()
    private var selectedPdfUri: Uri? = null
    private var savedPdfPath: String? = null
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePdfPickerResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedItems = it.getString("selectedItems")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        givenUserIsAtFormPage()
        setupUI()
        loadUserPhoneNumber()
        setupClickListeners()
        populateSelectedItems()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman formulir peminjaman barang
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        scenarioContext.validationErrors = emptyList()
        Log.d(TAG, "BDD - GIVEN: User is at form peminjaman barang page")
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
        val validationResult = validateCompleteFormData()
        scenarioContext.isFormDataComplete = validationResult.isValid
        scenarioContext.validationErrors = validationResult.errors

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
        val validationResult = validateCompleteFormData()
        scenarioContext.isFormDataComplete = validationResult.isValid
        scenarioContext.validationErrors = validationResult.errors

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
        FormUtils.resetButton(binding.btnSubmit, R.string.submit_button, requireContext())
    }

    // ==================== BDD HELPER METHODS ====================

    private fun getFormDataForBDD(): PeminjamanFormData {
        return PeminjamanFormData(
            namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim(),
            tujuanPeminjaman = binding.tujuanPeminjamanLayout.editText?.text.toString().trim(),
            harapanAnda = binding.harapanAndaLayout.editText?.text.toString().trim(),
            namaPJ = binding.namaPenanggungJawabLayout.editText?.text.toString().trim(),
            kontakPJ = binding.kontakPenanggungJawabLayout.editText?.text.toString().trim()
        )
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    private fun validateCompleteFormData(): ValidationResult {
        val errors = mutableListOf<String>()
        val data = scenarioContext.formData

        // Validasi field wajib
        if (data.namaPerangkat.isEmpty()) {
            errors.add("Nama perangkat tidak boleh kosong")
        }

        if (data.tujuanPeminjaman.isEmpty()) {
            errors.add("Tujuan peminjaman tidak boleh kosong")
        }

        if (startDate == null) {
            errors.add("Tanggal mulai tidak boleh kosong")
        }

        if (endDate == null) {
            errors.add("Tanggal selesai tidak boleh kosong")
        }

        if (data.harapanAnda.isEmpty()) {
            errors.add("Harapan tidak boleh kosong")
        }

        if (data.namaPJ.isEmpty()) {
            errors.add("Nama penanggung jawab tidak boleh kosong")
        }

        if (data.kontakPJ.isEmpty()) {
            errors.add("Kontak penanggung jawab tidak boleh kosong")
        } else if (!isValidPhone(data.kontakPJ)) {
            errors.add("Format kontak tidak valid")
        }

        // Validasi tanggal
        if (startDate != null && endDate != null) {
            val today = Date()
            if (startDate!!.before(today)) {
                errors.add("Tanggal mulai tidak boleh di masa lalu")
            }
            if (endDate!!.before(startDate)) {
                errors.add("Tanggal selesai tidak boleh sebelum tanggal mulai")
            }
        }

        Log.d(TAG, "BDD - Form validation errors: $errors")

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun isValidPhone(phone: String): Boolean {
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
        val selectedBarang = boxViewModel.getSelectedItems()
        val grupId = UUID.randomUUID().toString()
        var successCount = 0
        val totalItems = selectedBarang.size

        selectedBarang.forEach { barang ->
            val peminjamanData = createPeminjamanData(
                formData = scenarioContext.formData,
                barang = barang,
                grupId = grupId,
                totalItems = totalItems
            )

            // Use FormUtils for consistent submission handling
            FormUtils.saveFormToFirestore(
                firestore = firestore,
                collectionName = "form_peminjaman",
                formData = peminjamanData,
                context = requireContext(),
                onSuccess = {
                    clearForm()
                    successCount++
                    if (successCount == totalItems) {
                        handleSubmissionSuccess(totalItems)
                    }
                },
                onFailure = {
                    Log.e(TAG, "BDD - ERROR: Form submission failed for ${getBarangName(barang)}")
                    thenUserExperiencesTechnicalError()
                    handleSubmissionFailure(getBarangName(barang))
                }
            )
        }
    }

    private fun showErrorMessageAndStayAtFormPage() {
        // Tampilkan pesan error "Harap lengkapi semua data yang wajib"
        val errorMessage = "Harap lengkapi semua data yang wajib"

        Toast.makeText(
            requireContext(),
            errorMessage,
            Toast.LENGTH_LONG
        ).show()

        // Highlight field yang error
        highlightErrorFields()

        // Reset button
        FormUtils.resetButton(binding.btnSubmit, R.string.submit_button, requireContext())

        Log.d(TAG, "BDD - Error message shown: $errorMessage")
    }

    private fun highlightErrorFields() {
        scenarioContext.validationErrors.forEach { error ->
            when {
                error.contains("Nama perangkat") -> {
                    binding.namaPerangkatLayout.error = error
                }
                error.contains("Tujuan peminjaman") -> {
                    binding.tujuanPeminjamanLayout.error = error
                }
                error.contains("Tanggal mulai") -> {
                    binding.tanggalMulaiLayout.error = error
                }
                error.contains("Tanggal selesai") -> {
                    binding.tanggalSelesaiLayout.error = error
                }
                error.contains("Harapan") -> {
                    binding.harapanAndaLayout.error = error
                }
                error.contains("Nama penanggung jawab") -> {
                    binding.namaPenanggungJawabLayout.error = error
                }
                error.contains("Kontak") -> {
                    binding.kontakPenanggungJawabLayout.error = error
                }
            }
        }
    }

    // ==================== ORIGINAL IMPLEMENTATION METHODS ====================

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupDateFields()
    }

    private fun setupDateFields() {
        // Setup tanggal mulai
        binding.tanggalMulaiLayout.editText?.apply {
            isFocusable = false
            isClickable = true
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener { showDatePicker(true) }
        }

        // Setup tanggal selesai
        binding.tanggalSelesaiLayout.editText?.apply {
            isFocusable = false
            isClickable = true
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener { showDatePicker(false) }
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        // Set initial date
        if (isStartDate && startDate != null) {
            calendar.time = startDate!!
        } else if (!isStartDate && endDate != null) {
            calendar.time = endDate!!
        }

        val dateListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.time

            if (isStartDate) {
                startDate = selectedDate
                binding.tanggalMulaiLayout.editText?.setText(dateFormat.format(selectedDate))
                binding.tanggalMulaiLayout.error = null

                // Reset end date if it's before start date
                if (endDate != null && endDate!!.before(selectedDate)) {
                    endDate = null
                    binding.tanggalSelesaiLayout.editText?.text?.clear()
                }
            } else {
                // Validate end date is not before start date
                if (startDate != null && selectedDate.before(startDate)) {
                    binding.tanggalSelesaiLayout.error = "Tanggal selesai tidak boleh sebelum tanggal mulai"
                    return@OnDateSetListener
                }

                endDate = selectedDate
                binding.tanggalSelesaiLayout.editText?.setText(dateFormat.format(selectedDate))
                binding.tanggalSelesaiLayout.error = null
            }
        }

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = today.timeInMillis
        if (!isStartDate && startDate != null) {
            datePickerDialog.datePicker.minDate = startDate!!.time
        }

        datePickerDialog.show()
    }

    private fun loadUserPhoneNumber() {
        FormUtils.loadUserPhoneNumber(
            firestore = firestore,
            isEditMode = false,
            currentContactText = binding.kontakPenanggungJawabLayout.editText?.text.toString()
        ) { phoneNumber ->
            binding.kontakPenanggungJawabLayout.editText?.setText(phoneNumber)
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
        val validationResult = validateCompleteFormData()
        scenarioContext.isFormDataComplete = validationResult.isValid
        scenarioContext.validationErrors = validationResult.errors

        if (scenarioContext.isFormDataComplete) {
            // BDD: WHEN - Skenario 1: User mengisi form lengkap
            whenUserFillsCompleteFormAndPressesSubmit()
        } else {
            // BDD: WHEN - Skenario 2: User mengisi form tidak lengkap
            whenUserFillsIncompleteFormAndPressesSubmit()
        }
    }

    private fun populateSelectedItems() {
        selectedItems?.let { items ->
            binding.namaPerangkatLayout.editText?.setText(items)
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

    private fun getBarangName(barang: Any): String {
        return when (barang) {
            is Barang -> barang.namaBarang
            else -> barang.toString()
        }
    }

    private fun createPeminjamanData(
        formData: PeminjamanFormData,
        barang: Any,
        grupId: String,
        totalItems: Int
    ): Map<String, Any> {
        // Extract proper data from barang object
        val (namaBarang, jenisBarang, photoUrl) = when (barang) {
            is Barang -> Triple(barang.namaBarang, barang.jenis, barang.photoUrl)
            else -> {
                // Fallback for other types - try to extract from string representation
                val barangStr = barang.toString()
                Triple(
                    extractValueFromString(barangStr, "namaBarang") ?: "Unknown Item",
                    extractValueFromString(barangStr, "jenis") ?: "",
                    extractValueFromString(barangStr, "photoUrl") ?: ""
                )
            }
        }

        return hashMapOf(
            "judul" to "Form Peminjaman",
            "namaPerangkat" to namaBarang,
            "jenisBarang" to jenisBarang,
            "tujuanPeminjaman" to formData.tujuanPeminjaman,
            "tanggalMulai" to com.google.firebase.Timestamp(startDate!!),
            "tanggalSelesai" to com.google.firebase.Timestamp(endDate!!),
            "harapanAnda" to formData.harapanAnda,
            "namaPenanggungJawab" to formData.namaPJ,
            "kontakPenanggungJawab" to formData.kontakPJ,
            "filePath" to (savedPdfPath ?: ""),
            "statusPeminjaman" to "diajukan",
            "grupPeminjaman" to grupId,
            "totalItemsInGroup" to totalItems,
            "photoUrl" to photoUrl
        )
    }

    private fun extractValueFromString(input: String, key: String): String? {
        return try {
            val regex = """$key[=:]([^,}]+)""".toRegex()
            val matchResult = regex.find(input)
            matchResult?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun handleSubmissionSuccess(totalItems: Int) {
        boxViewModel.clearBox()
        FormUtils.resetButton(binding.btnSubmit, R.string.submit_button, requireContext())

        lifecycleScope.launch {
            // BDD: THEN - Pesan konfirmasi berhasil
            val successMessage = "Peminjaman $totalItems barang berhasil diajukan! Menunggu persetujuan admin."
            Toast.makeText(
                requireContext(),
                successMessage,
                Toast.LENGTH_LONG
            ).show()

            Log.d(TAG, "BDD - SUCCESS: $successMessage")
            findNavController().navigate(R.id.action_formPeminjamanFragment_to_historyPeminjamanBarangFragment)
        }
    }

    private fun handleSubmissionFailure(namaBarang: String) {
        FormUtils.resetButton(binding.btnSubmit, R.string.submit_button, requireContext())
        Toast.makeText(
            requireContext(),
            "Gagal mengirim peminjaman untuk $namaBarang",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun clearForm() {
        binding.namaPerangkatLayout.editText?.text?.clear()
        binding.tujuanPeminjamanLayout.editText?.text?.clear()
        binding.harapanAndaLayout.editText?.text?.clear()
        binding.namaPenanggungJawabLayout.editText?.text?.clear()
        binding.kontakPenanggungJawabLayout.editText?.text?.clear()
        binding.tanggalMulaiLayout.editText?.text?.clear()
        binding.tanggalSelesaiLayout.editText?.text?.clear()
        resetFileSelection()
        startDate = null
        endDate = null

        // Clear BDD context
        scenarioContext.formData = PeminjamanFormData("", "", "", "", "")
        scenarioContext.isFormDataComplete = false
        scenarioContext.validationErrors = emptyList()
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

    data class PeminjamanFormData(
        val namaPerangkat: String,
        val tujuanPeminjaman: String,
        val harapanAnda: String,
        val namaPJ: String,
        val kontakPJ: String
    )

    companion object {
        private const val TAG = "FormPeminjamanFragment"
    }
}