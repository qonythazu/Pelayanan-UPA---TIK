package com.dicoding.pelayananupa_tik.fragment

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPengaduanLayananBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPengaduanLayananFragment : Fragment() {

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
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                if (isFileSizeValid(uri)) {
                    val fileName = getFileName(uri)
                    binding.tvFileName.text = getString(R.string.file_selected, " $fileName")
                    binding.btnChooseFile.apply {
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        text = getString(R.string.change_image)
                        strokeWidth = 0
                    }
                    savePdfLocally(uri)
                } else {
                    selectedPdfUri = null
                    Toast.makeText(
                        requireContext(),
                        "File terlalu besar! Maksimal ukuran file adalah 2MB.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
        checkEditMode()
        // Load user phone number automatically
        loadUserPhoneNumber()
        binding.btnChooseFile.setOnClickListener { openPdfPicker() }
        binding.btnSubmit.setOnClickListener {
            if (isEditMode) {
                updateForm()
            } else {
                submitForm()
            }
        }
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        if (isEditMode) {
            binding.textView.text = getString(R.string.edit_pengaduan_layanan)
            binding.btnSubmit.text = getString(R.string.update)
        }
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
            val layanan = args.getString("layanan")
            val kontak = args.getString("kontak")
            val keluhan = args.getString("keluhan")
            val filePath = args.getString("filePath")

            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    layanan = layanan ?: "",
                    kontak = kontak ?: "",
                    keluhan = keluhan ?: "",
                    filePath = filePath ?: ""
                )
                binding.root.post {
                    populateFormForEdit()
                }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.layananLayout.editText?.setText(item.layanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.keluhanAndaLayout.editText?.setText(item.keluhan)

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

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Pilih File PDF"))
    }

    private fun isFileSizeValid(uri: Uri): Boolean {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()

            fileSize <= MAX_FILE_SIZE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file.pdf"
    }

    private fun savePdfLocally(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            savedPdfPath = file.absolutePath
            Toast.makeText(requireContext(), "File berhasil disimpan", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Hapus semua karakter non-digit
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

        // Cek panjang minimal dan maksimal
        if (digitsOnly.length < 10 || digitsOnly.length > 15) {
            return false
        }

        // Validasi format nomor Indonesia
        return when {
            // Format +62 (kode negara Indonesia)
            digitsOnly.startsWith("62") -> {
                val localNumber = digitsOnly.substring(2)
                isValidIndonesianLocalNumber(localNumber)
            }
            // Format 0 (format lokal Indonesia)
            digitsOnly.startsWith("0") -> {
                val localNumber = digitsOnly.substring(1)
                isValidIndonesianLocalNumber(localNumber)
            }
            // Format tanpa awalan (langsung nomor operator)
            else -> {
                isValidIndonesianLocalNumber(digitsOnly)
            }
        }
    }

    private fun isValidIndonesianLocalNumber(localNumber: String): Boolean {
        // Cek panjang nomor lokal (9-13 digit setelah kode area/operator)
        if (localNumber.length < 9 || localNumber.length > 13) {
            return false
        }

        // Validasi prefix operator Indonesia
        val validPrefixes = listOf(
            // Telkomsel
            "811", "812", "813", "821", "822", "823", "851", "852", "853",
            // Indosat
            "814", "815", "816", "855", "856", "857", "858",
            // XL
            "817", "818", "819", "859", "877", "878",
            // Tri (3)
            "895", "896", "897", "898", "899",
            // Smartfren
            "881", "882", "883", "884", "885", "886", "887", "888", "889",
            // Axis
            "831", "832", "833", "838",
            // Telkom (PSTN)
            "21", "22", "24", "31", "341", "343", "361", "370", "380", "401", "411", "421", "431", "451", "471", "481", "511", "541", "561", "571", "601", "620", "651", "717", "721", "741", "751", "761", "771", "778"
        )

        return validPrefixes.any { prefix -> localNumber.startsWith(prefix) }
    }

    private fun validateForm(formData: Triplet<String, String, String>): Boolean {
        val (layanan, kontak, keluhan) = formData
        var isValid = true
        if (layanan.isBlank()) {
            binding.layananLayout.error = "Jumlah tidak boleh kosong"
            isValid = false
        } else {
            binding.layananLayout.error = null
        }

        if (kontak.isBlank()) {
            binding.kontakLayout.error = "Kontak tidak boleh kosong"
            isValid = false
        } else if (!isValidPhoneNumber(kontak)) {
            binding.kontakLayout.error = "Format nomor telepon tidak valid"
            isValid = false
        } else {
            binding.kontakLayout.error = null
        }

        if (keluhan.isBlank()) {
            binding.keluhanAndaLayout.error = "Tujuan tidak boleh kosong"
            isValid = false
        } else {
            binding.keluhanAndaLayout.error = null
        }

        selectedPdfUri?.let { uri ->
            if (!isFileSizeValid(uri)) {
                Toast.makeText(
                    requireContext(),
                    "File yang dipilih terlalu besar! Maksimal ukuran file adalah 2MB.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }

        return isValid
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
                updateDataInFirestore(item.documentId, formData.first, formData.second, formData.third)
            } else {
                Toast.makeText(requireContext(), "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
        } ?: run {
            Toast.makeText(requireContext(), "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.update)
        }
    }

    private fun getFormData(): Triplet<String, String, String> {
        val layanan = binding.layananLayout.editText?.text.toString().trim()
        val kontak = binding.kontakLayout.editText?.text.toString().trim()
        val keluhan = binding.keluhanAndaLayout.editText?.text.toString().trim()

        return Triplet(layanan, kontak, keluhan)
    }

    private fun saveDataToFirestore(layanan: String, kontak: String, keluhan: String) {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))
        val userEmail = UserManager.getCurrentUserEmail()
        val pengaduan = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Pengaduan Layanan",
            "layanan" to layanan,
            "kontak" to kontak,
            "keluhan" to keluhan,
            "filePath" to (savedPdfPath ?: ""),
            "status" to "draft",
            "timestamp" to formattedDate
        )

        firestore.collection("form_pengaduan")
            .add(pengaduan)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPengaduanLayananFragment_to_historyLayananFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDataInFirestore(
        documentId: String,
        layanan: String,
        kontak: String,
        keluhan: String
    ) {
        if (documentId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Document ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.updating)

        val updateData = hashMapOf<String, Any>(
            "layanan" to layanan,
            "kontak" to kontak,
            "keluhan" to keluhan,
            "filePath" to (savedPdfPath ?: editingItem?.filePath ?: ""),
            "lastUpdated" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        firestore.collection("form_pengaduan")
            .document(documentId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)

                findNavController().previousBackStackEntry?.savedStateHandle?.set("data_updated", true)
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    findNavController().navigate(R.id.action_formPengaduanLayananFragment_to_historyLayananFragment)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Gagal mengupdate data: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
    }

    private fun clearForm() {
        binding.layananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keluhanAndaLayout.editText?.text?.clear()
        binding.tvFileName.text = ""
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
        }
        selectedPdfUri = null
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

    private companion object {
        private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
    }
}