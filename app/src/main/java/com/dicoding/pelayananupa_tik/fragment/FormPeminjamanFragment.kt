package com.dicoding.pelayananupa_tik.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.viewmodel.BoxViewModel
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPeminjamanBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
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

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                val fileName = getFileName(uri)
                binding.tvFileName.text = getString(R.string.file_selected, " $fileName")
                binding.btnChooseFile.text = getString(R.string.change_file)

                savePdfLocally(uri)
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPeminjamanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        setupViews()
        setupClickListeners()

        selectedItems?.let { items ->
            val namaPerangkatLayout = binding.namaPerangkatLayout
            namaPerangkatEditText = namaPerangkatLayout.editText as? TextInputEditText ?: return
            namaPerangkatEditText.setText(items)
        }
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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

    private fun validateForm(): Boolean {
        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim()
        val tujuanPeminjaman = binding.tujuanPeminjamanLayout.editText?.text.toString().trim()
        val harapanAnda = binding.harapanAndaLayout.editText?.text.toString().trim()
        val namaPJ = binding.namaPenanggungJawabLayout.editText?.text.toString().trim()
        val kontakPJ = binding.kontakPenanggungJawabLayout.editText?.text.toString().trim()

        when {
            namaPerangkat.isEmpty() -> {
                binding.namaPerangkatLayout.error = "Nama perangkat tidak boleh kosong"
                return false
            }
            tujuanPeminjaman.isEmpty() -> {
                binding.tujuanPeminjamanLayout.error = "Tujuan peminjaman tidak boleh kosong"
                return false
            }
            harapanAnda.isEmpty() -> {
                binding.harapanAndaLayout.error = "Harapan tidak boleh kosong"
                return false
            }
            namaPJ.isEmpty() -> {
                binding.namaPenanggungJawabLayout.error = "Nama penanggung jawab tidak boleh kosong"
                return false
            }
            kontakPJ.isEmpty() -> {
                binding.kontakPenanggungJawabLayout.error = "Kontak penanggung jawab tidak boleh kosong"
                return false
            }
            else -> {
                binding.namaPerangkatLayout.error = null
                binding.tujuanPeminjamanLayout.error = null
                binding.harapanAndaLayout.error = null
                binding.namaPenanggungJawabLayout.error = null
                binding.kontakPenanggungJawabLayout.error = null
                return true
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

    private fun submitForm() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.submitting)

        val namaPerangkat = binding.namaPerangkatLayout.editText?.text.toString().trim()
        val tujuanPeminjaman = binding.tujuanPeminjamanLayout.editText?.text.toString().trim()
        val harapanAnda = binding.harapanAndaLayout.editText?.text.toString().trim()
        val namaPJ = binding.namaPenanggungJawabLayout.editText?.text.toString().trim()
        val kontakPJ = binding.kontakPenanggungJawabLayout.editText?.text.toString().trim()

        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))

        val userEmail = UserManager.getCurrentUserEmail()
        val peminjamanData = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Peminjaman",
            "namaPerangkat" to namaPerangkat,
            "tujuanPeminjaman" to tujuanPeminjaman,
            "harapanAnda" to harapanAnda,
            "namaPenanggungJawab" to namaPJ,
            "kontakPenanggungJawab" to kontakPJ,
            "filePath" to (savedPdfPath ?: ""),
            "statusPeminjaman" to "Diajukan",
            "tanggalPengajuan" to formattedDate,
            "timestamp" to currentTime,
            "barangDipinjam" to getSelectedItemsList()
        )

        firestore.collection("form_peminjaman")
            .add(peminjamanData)
            .addOnSuccessListener { documentReference ->
                val peminjamanId = documentReference.id

                updateBarangStatus(peminjamanId) { success ->
                    if (success) {
                        boxViewModel.clearBox()

                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Peminjaman berhasil diajukan!", Toast.LENGTH_LONG).show()

                            findNavController().previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
                            findNavController().popBackStack()
                            findNavController().navigate(R.id.productListFragment)
                        }
                    } else {
                        binding.btnSubmit.isEnabled = true
                        binding.btnSubmit.text = getString(R.string.submit_button) // Gunakan string resource
                        Toast.makeText(requireContext(), "Gagal mengupdate status barang", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.submit_button)
                Toast.makeText(requireContext(), "Gagal mengirim peminjaman: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getSelectedItemsList(): List<Map<String, Any>> {
        val selectedBarang = boxViewModel.getSelectedItems()
        return selectedBarang.map { barang ->
            mapOf(
                "namaBarang" to barang.namaBarang,
                "jenis" to barang.jenis,
                "tanggalPinjam" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            )
        }
    }

    private fun updateBarangStatus(peminjamanId: String, callback: (Boolean) -> Unit) {
        val selectedBarang = boxViewModel.getSelectedItems()
        var completedUpdates = 0
        val totalUpdates = selectedBarang.size
        var hasError = false

        if (totalUpdates == 0) {
            callback(true)
            return
        }

        val userEmail = UserManager.getCurrentUserEmail()
        selectedBarang.forEach { barang ->
            firestore.collection("daftar_barang")
                .whereEqualTo("namaBarang", barang.namaBarang)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestore.collection("daftar_barang").document(document.id)
                            .update(
                                mapOf(
                                    "status" to "dipinjam",
                                    "peminjamanId" to peminjamanId,
                                    "tanggalDipinjam" to com.google.firebase.Timestamp.now(),
                                    "peminjam" to userEmail
                                )
                            )
                            .addOnCompleteListener {
                                completedUpdates++
                                if (!it.isSuccessful) hasError = true

                                if (completedUpdates == totalUpdates) {
                                    callback(!hasError)
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    hasError = true
                    completedUpdates++
                    if (completedUpdates == totalUpdates) {
                        callback(false)
                    }
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
}