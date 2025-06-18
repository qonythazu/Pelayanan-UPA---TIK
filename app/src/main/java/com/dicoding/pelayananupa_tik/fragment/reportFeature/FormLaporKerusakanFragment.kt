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
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FormLaporKerusakanFragment : Fragment() {

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

        checkEditMode()
        setupUI()
        loadUserPhoneNumber()
        setupClickListeners()
    }

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
            handleFormSubmission()
        }
    }

    private fun handleFormSubmission() {
        val formData = getFormData()
        val validationRules = buildValidation {
            required(formData.first, binding.namaPerangkatLayout, "Nama perangkat tidak boleh kosong")
            phone(formData.second, binding.kontakLayout)
            required(formData.third, binding.keteranganLayout, "Keterangan kerusakan tidak boleh kosong")
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "namaPerangkat" to formData.first,
                "kontak" to formData.second,
                "keterangan" to formData.third,
                "serialNumber" to serialNumber,
                "imagePath" to (savedImagePath ?: "")
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules
            ).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun submitNewForm(formData: Triple<String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Laporan Kerusakan",
            "namaPerangkat" to formData.first,
            "serialNumber" to serialNumber,
            "kontak" to formData.second,
            "keterangan" to formData.third,
            "imagePath" to (savedImagePath ?: "")
        )

        FormUtils.saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_lapor_kerusakan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formLaporKerusakanFragment_to_historyLayananFragment)
            },
            onFailure = {
                FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
            }
        )
    }

    private fun updateExistingForm(formData: Triple<String, String, String>) {
        FormUtils.handleEditModeError(
            editingItem = editingItem,
            submitButton = binding.btnSubmit,
            context = requireContext()
        ) { documentId ->
            val updateData = mapOf(
                "namaPerangkat" to formData.first,
                "kontak" to formData.second,
                "keterangan" to formData.third,
                "imagePath" to (savedImagePath ?: editingItem?.imagePath ?: "")
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_lapor_kerusakan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formLaporKerusakanFragment_to_historyLayananFragment
                    )
                },
                onFailure = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                }
            )
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

    private fun getFormData(): Triple<String, String, String> {
        return Triple(
            binding.namaPerangkatLayout.editText?.text.toString().trim(),
            binding.kontakLayout.editText?.text.toString().trim(),
            binding.keteranganLayout.editText?.text.toString().trim()
        )
    }

    private fun clearForm() {
        binding.namaPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keteranganLayout.editText?.text?.clear()
        resetImageSelection()
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