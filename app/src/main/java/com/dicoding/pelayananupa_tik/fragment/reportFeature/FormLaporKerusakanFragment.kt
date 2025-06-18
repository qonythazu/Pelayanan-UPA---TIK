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
import com.dicoding.pelayananupa_tik.helper.isValidPhoneNumber
import com.dicoding.pelayananupa_tik.utils.ProgressDialogFragment
import com.dicoding.pelayananupa_tik.utils.UserManager
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
    private var progressDialog: ProgressDialogFragment? = null
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

        // Load user phone number automatically
        loadUserPhoneNumber()
        setupViews()
        setupUI()
        setupClickListeners()
    }

    private fun loadUserPhoneNumber() {
        val userEmail = UserManager.getCurrentUserEmail()
        if (!userEmail.isNullOrEmpty()) {
            firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userDocument = documents.first()
                        val nomorTelepon = userDocument.getString("nomorTelepon")

                        // Hanya isi otomatis jika bukan mode edit atau field kosong
                        if (!isEditMode || binding.kontakLayout.editText?.text.toString().trim().isEmpty()) {
                            nomorTelepon?.let { phoneNumber ->
                                if (phoneNumber.isNotEmpty()) {
                                    binding.kontakLayout.editText?.setText(phoneNumber)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            val namaPerangkat = args.getString("namaPerangkat")
            val kontak = args.getString("kontak")
            val keterangan = args.getString("keterangan")
            val imagePath = args.getString("imagePath")

            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    namaPerangkat = namaPerangkat ?: "",
                    kontak = kontak ?: "",
                    keterangan = keterangan ?: "",
                    imagePath = imagePath ?: ""
                )
                binding.root.post {
                    populateFormForEdit()
                }
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
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        text = getString(R.string.change_image)
                        strokeWidth = 0
                    }
                    savedImagePath = item.imagePath
                }
            }
        }
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        if (isEditMode) {
            binding.textView.text = getString(R.string.edit_laporan_kerusakan)
            binding.btnSubmit.text = getString(R.string.update)
        }
    }

    private fun setupUI() {
        binding.namaPerangkatLayout.editText?.setText(namaPerangkat)
        binding.tvFileName.text = getString(R.string.no_file_selected)
    }

    private fun setupClickListeners() {
        binding.btnChooseFile.setOnClickListener {
            showImagePickerDialog()
        }
        binding.btnSubmit.setOnClickListener {
            if (isEditMode) {
                updateForm()
            } else {
                submitForm()
            }
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
            binding.tvFileName.text = getString(R.string.file_selected, " $fileName")

            binding.btnChooseFile.apply {
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                text = getString(R.string.change_image)
                strokeWidth = 0
            }

            saveImageLocally(uri)
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

    private fun validateForm(formData: Triple<String, String, String>): Boolean {
        val (namaPerangkat, kontak, keterangan) = formData

        if (namaPerangkat.isBlank()) {
            binding.namaPerangkatLayout.error = "Nama perangkat tidak boleh kosong"
            return false
        } else {
            binding.namaPerangkatLayout.error = null
        }

        if (kontak.isBlank()) {
            binding.kontakLayout.error = "Kontak penanggung jawab tidak boleh kosong"
            return false
        } else if (!isValidPhoneNumber(kontak)) {
            binding.kontakLayout.error = "Kontak harus berupa nomor dan minimal 10 digit"
            return false
        } else {
            binding.kontakLayout.error = null
        }

        if (keterangan.isBlank()) {
            binding.keteranganLayout.error = "Keterangan kerusakan tidak boleh kosong"
            return false
        } else {
            binding.keteranganLayout.error = null
        }

        return true
    }

    private fun submitForm() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.submitting)
        val formData = getFormData()
        if (!validateForm(formData)) {
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.submit)
            return
        }

        showLoading()
        saveDataToFirestore(formData.first, formData.second, formData.third)
    }

    private fun updateForm() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.submitting)
        val formData = getFormData()
        if (!validateForm(formData)) {
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.update)
            return
        }

        editingItem?.let { item ->
            if (item.documentId.isNotEmpty()) {
                showLoading()
                updateDataInFirestore(item.documentId, formData.first, formData.second, formData.third)
            } else {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
                Toast.makeText(requireContext(), "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.update)
            Toast.makeText(requireContext(), "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFormData(): Triple<String, String, String> {
        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim()
        val kontak = binding.kontakLayout.editText?.text.toString().trim()
        val keterangan = binding.keteranganLayout.editText?.text.toString().trim()

        return Triple(namaPerangkat, kontak, keterangan)
    }

    private fun saveDataToFirestore(namaPerangkat: String, kontak: String, keterangan: String) {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))
        val userEmail = UserManager.getCurrentUserEmail()

        val laporanData = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Laporan Kerusakan",
            "namaPerangkat" to namaPerangkat,
            "serialNumber" to serialNumber,
            "kontak" to kontak,
            "keterangan" to keterangan,
            "timestamp" to formattedDate,
            "status" to "draft",
            "imagePath" to (savedImagePath ?: "")
        )

        firestore.collection("form_lapor_kerusakan")
            .add(laporanData)
            .addOnSuccessListener { documentReference ->
                hideLoading()
                Log.d(TAG, "Laporan saved with ID: ${documentReference.id}")

                Toast.makeText(
                    requireContext(),
                    "Laporan kerusakan berhasil dikirim",
                    Toast.LENGTH_LONG
                ).show()
                clearForm()
                findNavController().navigate(R.id.action_formLaporKerusakanFragment_to_historyLayananFragment)
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e(TAG, "Error saving laporan", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal mengirim laporan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateDataInFirestore(
        documentId: String,
        namaPerangkat: String,
        kontak: String,
        keterangan: String
    ) {
        if (documentId.isEmpty()) {
            hideLoading()
            Toast.makeText(requireContext(), "Error: Document ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.updating)

        val updateData = hashMapOf<String, Any>(
            "namaPerangkat" to namaPerangkat,
            "kontak" to kontak,
            "keterangan" to keterangan,
            "imagePath" to (savedImagePath ?: editingItem?.imagePath ?: ""),
            "lastUpdated" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        firestore.collection("form_lapor_kerusakan")
            .document(documentId)
            .update(updateData)
            .addOnSuccessListener {
                hideLoading()
                Toast.makeText(requireContext(), "Data berhasil diupdate", Toast.LENGTH_SHORT).show()

                // Reset button state
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)

                findNavController().previousBackStackEntry?.savedStateHandle?.set("data_updated", true)
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    // Fallback navigation
                    findNavController().navigate(R.id.action_formLaporKerusakanFragment_to_historyLayananFragment)
                }
            }
            .addOnFailureListener { exception ->
                hideLoading()
                Toast.makeText(requireContext(), "Gagal mengupdate data: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
    }

    private fun clearForm() {
        binding.namaPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keteranganLayout.editText?.text?.clear()
        binding.tvFileName.text = getString(R.string.no_file_selected)

        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
        }

        imageUri = null
        savedImagePath = null
    }

    private fun showLoading() {
        if (!isAdded) return

        try {
            progressDialog = ProgressDialogFragment()
            val fm = parentFragmentManager
            if (!fm.isDestroyed && !fm.isStateSaved) {
                progressDialog?.show(fm, "loading")
                Log.d(TAG, "Loading dialog shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading dialog", e)
        }
    }

    private fun hideLoading() {
        try {
            progressDialog?.let { dialog ->
                if (dialog.isAdded) {
                    dialog.dismissAllowingStateLoss()
                }
            }
            progressDialog = null
            Log.d(TAG, "Loading dialog hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading dialog", e)
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
        hideLoading()
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