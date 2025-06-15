package com.dicoding.pelayananupa_tik.fragment

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPeminjamanBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FormPeminjamanFragment : Fragment() {

    private var _binding: FragmentFormPeminjamanBinding? = null
    private val binding get() = _binding!!
    private var selectedItems: String? = null
    private lateinit var namaPerangkatEditText: TextInputEditText
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

        // Load user phone number automatically
        loadUserPhoneNumber()

        setupViews()
        setupClickListeners()

        selectedItems?.let { items ->
            val namaPerangkatLayout = binding.namaPerangkatLayout
            namaPerangkatEditText = namaPerangkatLayout.editText as? TextInputEditText ?: return
            namaPerangkatEditText.setText(items)
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
                        if (binding.kontakPenanggungJawabLayout.editText?.text.toString().trim().isEmpty()) {
                            nomorTelepon?.let { phoneNumber ->
                                if (phoneNumber.isNotEmpty()) {
                                    binding.kontakPenanggungJawabLayout.editText?.setText(phoneNumber)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        setupDateRangePicker()
    }

    private fun setupDateRangePicker() {
        val rentangTanggalEditText = binding.rentangTanggalLayout.editText
        rentangTanggalEditText?.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { showDateRangePicker() }
        }
    }

    private fun showDateRangePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal Peminjaman")
            .setSelection(
                androidx.core.util.Pair(
                    startDate?.time ?: today,
                    endDate?.time ?: (today + (7 * 24 * 60 * 60 * 1000))
                )
            )
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            startDate = Date(selection.first)
            endDate = Date(selection.second)

            val startDateStr = dateFormat.format(startDate!!)
            val endDateStr = dateFormat.format(endDate!!)
            val dateRangeStr = "$startDateStr - $endDateStr"

            binding.rentangTanggalLayout.editText?.setText(dateRangeStr)
            binding.rentangTanggalLayout.error = null
        }

        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }

        binding.btnChooseFile.setOnClickListener {
            openPdfPicker()
        }
    }

    private fun clearAllErrors() {
        binding.namaPerangkatLayout.error = null
        binding.tujuanPeminjamanLayout.error = null
        binding.rentangTanggalLayout.error = null
        binding.harapanAndaLayout.error = null
        binding.namaPenanggungJawabLayout.error = null
        binding.kontakPenanggungJawabLayout.error = null
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
            val fileName = "peminjaman_${System.currentTimeMillis()}.pdf"
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

    private fun validateForm(): Boolean {
        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim()
        val tujuanPeminjaman = binding.tujuanPeminjamanLayout.editText?.text.toString().trim()
        val rentangTanggal = binding.rentangTanggalLayout.editText?.text.toString().trim()
        val harapanAnda = binding.harapanAndaLayout.editText?.text.toString().trim()
        val namaPJ = binding.namaPenanggungJawabLayout.editText?.text.toString().trim()
        val kontakPJ = binding.kontakPenanggungJawabLayout.editText?.text.toString().trim()
        clearAllErrors()

        return when {
            namaPerangkat.isEmpty() -> {
                binding.namaPerangkatLayout.error = "Nama perangkat tidak boleh kosong"
                false
            }
            tujuanPeminjaman.isEmpty() -> {
                binding.tujuanPeminjamanLayout.error = "Tujuan peminjaman tidak boleh kosong"
                false
            }
            rentangTanggal.isEmpty() || startDate == null || endDate == null -> {
                binding.rentangTanggalLayout.error = "Rentang tanggal tidak boleh kosong"
                false
            }
            startDate!!.before(Date()) -> {
                binding.rentangTanggalLayout.error = "Tanggal mulai tidak boleh di masa lalu"
                false
            }
            harapanAnda.isEmpty() -> {
                binding.harapanAndaLayout.error = "Harapan tidak boleh kosong"
                false
            }
            namaPJ.isEmpty() -> {
                binding.namaPenanggungJawabLayout.error = "Nama penanggung jawab tidak boleh kosong"
                false
            }
            kontakPJ.isEmpty() -> {
                binding.kontakPenanggungJawabLayout.error = "Kontak penanggung jawab tidak boleh kosong"
                false
            }
            !isValidPhoneNumber(kontakPJ) -> {
                binding.kontakPenanggungJawabLayout.error = "Kontak harus berupa nomor dan minimal 10 digit"
                false
            }
            else -> true
        }
    }

    private fun submitForm() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.submitting)

        val tujuanPeminjaman = binding.tujuanPeminjamanLayout.editText?.text.toString().trim()
        val rentangTanggal = binding.rentangTanggalLayout.editText?.text.toString().trim()
        val harapanAnda = binding.harapanAndaLayout.editText?.text.toString().trim()
        val namaPJ = binding.namaPenanggungJawabLayout.editText?.text.toString().trim()
        val kontakPJ = binding.kontakPenanggungJawabLayout.editText?.text.toString().trim()
        val currentTime = System.currentTimeMillis()
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateTimeFormat.format(Date(currentTime))
        val userEmail = UserManager.getCurrentUserEmail()
        val grupId = UUID.randomUUID().toString()
        val selectedBarang = boxViewModel.getSelectedItems()
        var successCount = 0
        val totalItems = selectedBarang.size
        selectedBarang.forEach { barang ->
            val peminjamanData = hashMapOf(
                "userEmail" to userEmail,
                "judul" to "Form Peminjaman",
                "namaPerangkat" to barang.namaBarang,
                "jenisBarang" to barang.jenis,
                "tujuanPeminjaman" to tujuanPeminjaman,
                "rentangTanggal" to rentangTanggal,
                "tanggalMulai" to com.google.firebase.Timestamp(startDate!!),
                "tanggalSelesai" to com.google.firebase.Timestamp(endDate!!),
                "harapanAnda" to harapanAnda,
                "namaPenanggungJawab" to namaPJ,
                "kontakPenanggungJawab" to kontakPJ,
                "filePath" to (savedPdfPath ?: ""),
                "statusPeminjaman" to "diajukan",
                "tanggalPengajuan" to formattedDate,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "grupPeminjaman" to grupId,
                "totalItemsInGroup" to totalItems,
                "photoUrl" to (barang.photoUrl)
            )

            firestore.collection("form_peminjaman")
                .add(peminjamanData)
                .addOnSuccessListener {
                    successCount++
                    if (successCount == totalItems) {
                        boxViewModel.clearBox()

                        lifecycleScope.launch {
                            Toast.makeText(
                                requireContext(),
                                "Peminjaman $totalItems barang berhasil diajukan! Menunggu persetujuan admin.",
                                Toast.LENGTH_LONG
                            ).show()
                            findNavController().navigate(R.id.action_formPeminjamanFragment_to_historyPeminjamanBarangFragment)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = getString(R.string.submit_button)
                    Toast.makeText(
                        requireContext(),
                        "Gagal mengirim peminjaman untuk ${barang.namaBarang}: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

    private companion object {
        private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
    }
}