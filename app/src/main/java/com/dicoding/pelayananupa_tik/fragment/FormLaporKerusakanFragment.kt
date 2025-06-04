package com.dicoding.pelayananupa_tik.fragment

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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormLaporKerusakanBinding
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

    private var imageUri: Uri? = null
    private var namaPerangkat: String = ""
    private var serialNumber: String = ""
    private var progressDialog: ProgressDialogFragment? = null

    private val firestore = FirebaseFirestore.getInstance()

    // Launcher untuk mengambil gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleImageResult(uri)
    }

    // Launcher untuk mengambil foto dari kamera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && imageUri != null) {
            handleImageResult(imageUri)
        }
    }

    // Launcher untuk meminta permission kamera
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

        setupViews()
        setupUI()
        setupClickListeners()

        Log.d(TAG, "Form setup completed")
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
            submitForm()
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
            binding.tvFileName.text = uri.lastPathSegment ?: "File selected"

            binding.btnChooseFile.apply {
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                text = getString(R.string.change_image)
                strokeWidth = 0
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10 && phoneNumber.matches(Regex("^[0-9+\\-\\s()]*$"))
    }

    private fun submitForm() {
        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString()
        val kontak = binding.kontakLayout.editText?.text.toString()
        val keterangan = binding.keteranganLayout.editText?.text.toString()
        val localImagePath = if (imageUri != null) {
            saveImageLocally()
        } else {
            null
        }

        when {
            namaPerangkat.isEmpty() -> {
                binding.namaPerangkatLayout.error = "Nama perangkat tidak boleh kosong"
                return
            }
            kontak.isEmpty() -> {
                binding.kontakLayout.error = "Kontak penanggung jawab tidak boleh kosong"
                return
            }
            !isValidPhoneNumber(kontak) -> {
                binding.kontakLayout.error = "Kontak harus berupa nomor dan minimal 10 digit"
                return
            }
            keterangan.isEmpty() -> {
                binding.keteranganLayout.error = "Keterangan kerusakan tidak boleh kosong"
                return
            }
            else -> {
                binding.namaPerangkatLayout.error = null
                binding.kontakLayout.error = null
                binding.keteranganLayout.error = null
                showLoading()
                saveDataToFirestore(namaPerangkat, kontak, keterangan, localImagePath)
            }
        }
    }

    private fun saveImageLocally(): String? {
        if (imageUri == null) return null

        try {
            val filename = "IMG_${UUID.randomUUID()}.jpg"
            val imagesDir = File(requireContext().filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdir()
            }

            val destinationFile = File(imagesDir, filename)

            requireContext().contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return destinationFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun saveDataToFirestore(namaPerangkat: String, kontak: String, keterangan: String, localImagePath: String?) {
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
            "status" to "Draft",
            "localImagePath" to localImagePath
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