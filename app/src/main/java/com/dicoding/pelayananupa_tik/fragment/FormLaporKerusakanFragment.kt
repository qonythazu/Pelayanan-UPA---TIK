package com.dicoding.pelayananupa_tik.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class FormLaporKerusakanFragment : Fragment() {

    private var _binding: FragmentFormLaporKerusakanBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var namaPerangkat: String = ""
    private var serialNumber: String = ""
    private var progressDialog: ProgressDialogFragment? = null

    private val firestore = FirebaseFirestore.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                val fileName = getFileName(uri)
                binding.tvFileName.text = fileName ?: "File terpilih"
                Log.d(TAG, "Image selected: $fileName")
            }
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

        setupUI()
        setupClickListeners()

        Log.d(TAG, "Form setup completed")
    }

    private fun setupUI() {
        binding.namaPerangkatLayout.editText?.setText(namaPerangkat)
        binding.tvFileName.text = getString(R.string.no_file_selected)
    }

    private fun setupClickListeners() {
        binding.btnChooseFile.setOnClickListener {
            openImagePicker()
        }
        binding.btnSubmit.setOnClickListener {
            submitLaporanKerusakan()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun submitLaporanKerusakan() {
        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim()
        val kontak = binding.kontakLayout.editText?.text.toString().trim()
        val keterangan = binding.keteranganLayout.editText?.text.toString().trim()

        if (namaPerangkat.isEmpty()) {
            binding.namaPerangkatLayout.error = "Nama perangkat harus diisi"
            return
        }

        if (kontak.isEmpty()) {
            binding.kontakLayout.error = "Kontak penanggung jawab harus diisi"
            return
        }

        if (keterangan.isEmpty()) {
            binding.keteranganLayout.error = "Keterangan kerusakan harus diisi"
            return
        }

        binding.namaPerangkatLayout.error = null
        binding.kontakLayout.error = null
        binding.keteranganLayout.error = null

        showLoading()

        if (selectedImageUri != null) {
            saveImageLocallyAndSaveData(namaPerangkat, kontak, keterangan)
        } else {
            saveDataToFirestore(namaPerangkat, kontak, keterangan, null)
        }
    }

    private fun saveImageLocallyAndSaveData(namaPerangkat: String, kontak: String, keterangan: String) {
        selectedImageUri?.let { uri ->
            try {
                val fileName = "laporan_kerusakan_${System.currentTimeMillis()}.jpg"
                val localImagePath = saveImageToInternalStorage(uri, fileName)

                if (localImagePath != null) {
                    saveDataToFirestore(namaPerangkat, kontak, keterangan, localImagePath)
                } else {
                    hideLoading()
                    Toast.makeText(requireContext(), "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideLoading()
                Log.e(TAG, "Error saving image locally", e)
                Toast.makeText(requireContext(), "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, "laporan_images")

            if (!file.exists()) {
                file.mkdirs()
            }

            val imageFile = File(file, fileName)
            val outputStream = FileOutputStream(imageFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Image saved locally: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to internal storage", e)
            null
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
            "nama_perangkat" to namaPerangkat,
            "serial_number" to serialNumber,
            "kontak_penanggung_jawab" to kontak,
            "keterangan_kerusakan" to keterangan,
            "tanggal_laporan" to formattedDate,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "status" to "Terkirim",
            "local_image_path" to localImagePath
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

                // Navigate back
                findNavController().popBackStack()
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

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                it.getString(nameIndex)
            } else null
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