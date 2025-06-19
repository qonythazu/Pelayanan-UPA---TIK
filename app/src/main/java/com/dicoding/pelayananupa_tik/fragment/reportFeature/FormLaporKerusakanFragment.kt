package com.dicoding.pelayananupa_tik.fragment.reportFeature

import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormLaporKerusakanBinding
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FormLaporKerusakanFragment : Fragment() {

    // ==================== BDD DATA CLASSES ====================
    private data class FormScenarioContext(
        var userIsAtFormPage: Boolean = false,
        var formData: FormData = FormData(),
        var isFormDataComplete: Boolean = false,
        var submitResult: SubmitResult = SubmitResult.PENDING
    )

    private data class FormData(
        var namaPerangkat: String = "",
        var kontak: String = "",
        var keterangan: String = "",
        var imagePath: String = ""
    )

    private enum class SubmitResult {
        PENDING, SUCCESS, FAILED_INCOMPLETE_DATA, FAILED_ERROR
    }

    // ==================== CLASS PROPERTIES ====================
    private val scenarioContext = FormScenarioContext()

    private var _binding: FragmentFormLaporKerusakanBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var imageUri: Uri? = null
    private var namaPerangkat: String = ""
    private var serialNumber: String = ""
    private var savedImagePath: String? = null
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleImageResult(uri)
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && imageUri != null) {
            handleImageResult(imageUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permission kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            namaPerangkat = args.getString("nama_perangkat") ?: ""
            serialNumber = args.getString("serial_number") ?: ""
            Log.d(TAG, "Data received - Nama Perangkat: $namaPerangkat, Serial: $serialNumber")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormLaporKerusakanBinding.inflate(inflater, container, false)
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
     * GIVEN: User telah login dan berada di halaman formulir lapor kerusakan
     */
    private fun givenUserIsAtFormPage() {
        scenarioContext.userIsAtFormPage = true
        scenarioContext.submitResult = SubmitResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at form lapor kerusakan page")
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
     * THEN: Gagal terkirim dan user melihat pesan error "Harap lengkapi semua data yang wajib"
     * dan user kembali ke halaman formulir kerusakan barang (Skenario 2)
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
            namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim(),
            kontak = binding.kontakLayout.editText?.text.toString().trim(),
            keterangan = binding.keteranganLayout.editText?.text.toString().trim(),
            imagePath = savedImagePath ?: ""
        )
    }

    private fun validateCompleteFormData(): Boolean {
        val data = scenarioContext.formData
        val isNamaPerangkatValid = data.namaPerangkat.isNotEmpty()
        val isKontakValid = data.kontak.isNotEmpty() && isValidPhone(data.kontak)
        val isKeteranganValid = data.keterangan.isNotEmpty()

        Log.d(TAG, "BDD - Form validation: namaPerangkat=$isNamaPerangkatValid, kontak=$isKontakValid, keterangan=$isKeteranganValid")

        return isNamaPerangkatValid && isKontakValid && isKeteranganValid
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
            "judul" to "Laporan Kerusakan",
            "namaPerangkat" to scenarioContext.formData.namaPerangkat,
            "serialNumber" to serialNumber,
            "kontak" to scenarioContext.formData.kontak,
            "keterangan" to scenarioContext.formData.keterangan,
            "imagePath" to scenarioContext.formData.imagePath
        )

        if (isEditMode) {
            executeUpdateForm(dataToSave)
        } else {
            executeNewFormSubmission(dataToSave)
        }
    }

    private fun executeNewFormSubmission(dataToSave: Map<String, String>) {
        FormUtils.saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_lapor_kerusakan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                Log.d(TAG, "BDD - SUCCESS: Form submitted successfully")
                showSuccessMessage("Laporan kerusakan berhasil dikirim!")
                clearForm()
                findNavController().navigate(R.id.action_formLaporKerusakanFragment_to_historyLayananFragment)
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
                collectionName = "form_lapor_kerusakan",
                documentId = documentId,
                updateData = dataToSave,
                context = requireContext(),
                onSuccess = {
                    Log.d(TAG, "BDD - SUCCESS: Form updated successfully")
                    showSuccessMessage("Laporan kerusakan berhasil diperbarui!")
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formLaporKerusakanFragment_to_historyLayananFragment
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
            if (scenarioContext.formData.namaPerangkat.isEmpty()) append("• Nama perangkat tidak boleh kosong\n")
            if (scenarioContext.formData.kontak.isEmpty()) append("• Kontak tidak boleh kosong\n")
            else if (!isValidPhone(scenarioContext.formData.kontak)) append("• Format kontak tidak valid\n")
            if (scenarioContext.formData.keterangan.isEmpty()) append("• Keterangan kerusakan tidak boleh kosong\n")
        }.trimEnd()

        Toast.makeText(
            requireContext(),
            errorMessage,
            Toast.LENGTH_LONG
        ).show()

        // Highlight field yang error
        highlightErrorFields()

        // Reset button
        FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
    }

    private fun highlightErrorFields() {
        if (scenarioContext.formData.namaPerangkat.isEmpty()) {
            binding.namaPerangkatLayout.error = "Nama perangkat tidak boleh kosong"
        } else {
            binding.namaPerangkatLayout.error = null
        }

        if (scenarioContext.formData.kontak.isEmpty()) {
            binding.kontakLayout.error = "Kontak tidak boleh kosong"
        } else if (!isValidPhone(scenarioContext.formData.kontak)) {
            binding.kontakLayout.error = "Format kontak tidak valid"
        } else {
            binding.kontakLayout.error = null
        }

        if (scenarioContext.formData.keterangan.isEmpty()) {
            binding.keteranganLayout.error = "Keterangan kerusakan tidak boleh kosong"
        } else {
            binding.keteranganLayout.error = null
        }
    }

    private fun showSuccessMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // ==================== ORIGINAL IMPLEMENTATION METHODS ====================

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_laporan_kerusakan)

        // Set nama perangkat dari arguments
        binding.namaPerangkatLayout.editText?.setText(namaPerangkat)
        binding.tvFileName.text = getString(R.string.no_file_selected)
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
            showImagePickerDialog()
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

    private fun showImagePickerDialog() {
        val options = arrayOf("Kamera", "Galeri")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val imageFile = createImageFile()
            imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )
            takePictureLauncher.launch(imageUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = File(requireContext().filesDir, "images")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun handleImageResult(uri: Uri?) {
        imageUri = uri
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "Gambar dipilih"
            updateImageSelection(fileName)
            saveImageLocally(uri)
        }
    }

    private fun updateImageSelection(fileName: String) {
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

    private fun saveImageLocally(uri: Uri) {
        try {
            val filename = "IMG_${UUID.randomUUID()}.jpg"
            val imagesDir = File(requireContext().filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdir()
            }

            val destinationFile = File(imagesDir, filename)

            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            savedImagePath = destinationFile.absolutePath
            Toast.makeText(requireContext(), "Gambar berhasil disimpan", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    namaPerangkat = args.getString("namaPerangkat") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    keterangan = args.getString("keterangan") ?: "",
                    imagePath = args.getString("imagePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.namaPerangkatLayout.editText?.setText(item.namaPerangkat)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.keteranganLayout.editText?.setText(item.keterangan)

            if (item.imagePath.isNotEmpty()) {
                val file = File(item.imagePath)
                if (file.exists()) {
                    binding.tvFileName.text = getString(R.string.file_selected, " ${file.name}")
                    binding.btnChooseFile.apply {
                        backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.primary_blue)
                        )
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        text = getString(R.string.change_image)
                        strokeWidth = 0
                    }
                    savedImagePath = item.imagePath
                }
            }
        }
    }

    private fun clearForm() {
        binding.namaPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keteranganLayout.editText?.text?.clear()
        resetImageSelection()

        // Clear BDD context
        scenarioContext.formData = FormData()
        scenarioContext.isFormDataComplete = false
    }

    private fun resetImageSelection() {
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
        imageUri = null
        savedImagePath = null
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
        private const val TAG = "FormLaporKerusakanFragment"

        fun newInstance(namaPerangkat: String, serialNumber: String): FormLaporKerusakanFragment {
            val fragment = FormLaporKerusakanFragment()
            val args = Bundle().apply {
                putString("nama_perangkat", namaPerangkat)
                putString("serial_number", serialNumber)
            }
            fragment.arguments = args
            return fragment
        }
    }
}