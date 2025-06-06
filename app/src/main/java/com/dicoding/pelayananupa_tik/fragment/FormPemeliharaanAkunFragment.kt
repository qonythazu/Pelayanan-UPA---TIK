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
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemeliharaanAkunBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPemeliharaanAkunFragment : Fragment() {

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
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                val fileName = getFileName(uri)
                binding.tvFileName.text = getString(R.string.file_selected, " $fileName")
                binding.btnChooseFile.text = getString(R.string.change_file)

                savePdfLocally(uri)
            }
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
        checkEditMode()
        binding.radioGroupLayanan.setOnCheckedChangeListener { _, _ -> }
        binding.radioGroupJenis.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
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
            binding.textView.text = getString(R.string.edit_pemeliharaan_akun)
            binding.btnSubmit.text = getString(R.string.update)
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            val layanan = args.getString("layanan")
            val jenis = args.getString("jenis")
            val akun = args.getString("akun")
            val alasan = args.getString("alasan")
            val filePath = args.getString("filePath")

            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    layanan = layanan ?: "",
                    jenis = jenis ?: "",
                    akun = akun ?: "",
                    alasan = alasan ?: "",
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
            val layananRadioButtons = mapOf(
                "Subdomain" to binding.radioSubDomain,
                "Hosting" to binding.radioHosting,
                "Virtual Private Server (VPS)" to binding.radioVPS,
                "Website" to binding.radioWebsite,
                "Email" to binding.radioEmail,
                "Gerbang ITK" to binding.radioGerbang,
                "Microsoft 365" to binding.radioMic
            )
            layananRadioButtons[item.layanan]?.isChecked = true
            val jenisRadioButtons = mapOf(
                "Reset Password Akun" to binding.radioReset,
                "Perubahan/Penambahan Data Layanan" to binding.radioPerubahan,
                "Penambahan Penyimpanan" to binding.radioPenambahan
            )
            if (jenisRadioButtons.containsKey(item.jenis)) {
                jenisRadioButtons[item.jenis]?.isChecked = true
            } else {
                binding.radioOther.isChecked = true
                binding.textInputLayoutOther.visibility = View.VISIBLE
                binding.editTextOther.setText(item.jenis)
            }
            binding.namaAkunLayout.editText?.setText(item.akun)
            binding.alasanLayout.editText?.setText(item.alasan)

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
        val formData = getFormData()
        if (!validateForm(formData)) return
        saveDataToFirestore(formData.first, formData.second, formData.third, formData.fourth)
    }

    private fun updateForm() {
        val formData = getFormData()
        if (!validateForm(formData)) return
        editingItem?.let { item ->
            if (item.documentId.isNotEmpty()) {
                updateDataInFirestore(item.documentId, formData.first, formData.second, formData.third, formData.fourth)
            } else {
                Toast.makeText(requireContext(), "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFormData(): Quadruple<String, String, String, String> {
        val selectedRadioButtonLayanan = binding.radioGroupLayanan.checkedRadioButtonId
        val selectedRadioButtonJenis = binding.radioGroupJenis.checkedRadioButtonId
        val layanan = if (selectedRadioButtonLayanan != -1) {
            view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
        } else ""
        val jenis = if (selectedRadioButtonJenis == R.id.radioOther) {
            binding.editTextOther.text.toString().trim()
        } else if (selectedRadioButtonJenis != -1) {
            view?.findViewById<RadioButton>(selectedRadioButtonJenis)?.text?.toString() ?: ""
        } else ""
        val akun = binding.namaAkunLayout.editText?.text.toString().trim()
        val alasan = binding.alasanLayout.editText?.text.toString().trim()

        return Quadruple(layanan, jenis, akun, alasan)
    }

    private fun validateForm(formData: Quadruple<String, String, String, String>): Boolean {
        val (layanan, jenis, akun, alasan) = formData

        if (layanan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap pilih layanan yang diajukan", Toast.LENGTH_SHORT).show()
            return false
        }

        if (jenis.isEmpty()) {
            Toast.makeText(requireContext(), "Harap pilih jenis pemeliharaan yang diajukan", Toast.LENGTH_SHORT).show()
            return false
        }

        if (akun.isBlank()) {
            binding.namaAkunLayout.error = "Nama Akun Layanan tidak boleh kosong"
            return false
        } else {
            binding.namaAkunLayout.error = null
        }

        if (alasan.isBlank()) {
            binding.alasanLayout.error = "Alasan Pemeliharaan tidak boleh kosong"
            return false
        } else {
            binding.alasanLayout.error = null
        }

        return true
    }

    private fun saveDataToFirestore(
        layanan: String,
        jenis: String,
        akun: String,
        alasan: String
    ) {
        val userEmail = UserManager.getCurrentUserEmail()
        val currentTime = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(currentTime))

        val pemeliharaan = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Pemeliharaan Akun",
            "layanan" to layanan,
            "jenis" to jenis,
            "akun" to akun,
            "alasan" to alasan,
            "filePath" to (savedPdfPath ?: ""),
            "status" to "Draft",
            "timestamp" to formattedDate
        )

        firestore.collection("form_pemeliharaan")
            .add(pemeliharaan)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDataInFirestore(
        documentId: String,
        layanan: String,
        jenis: String,
        akun: String,
        alasan: String
    ) {
        if (documentId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Document ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.updating)

        val updateData = hashMapOf<String, Any>(
            "layanan" to layanan,
            "jenis" to jenis,
            "akun" to akun,
            "alasan" to alasan,
            "filePath" to (savedPdfPath ?: editingItem?.filePath ?: ""),
            "lastUpdated" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        firestore.collection("form_pemeliharaan")
            .document(documentId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                // Reset button state
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)

                findNavController().previousBackStackEntry?.savedStateHandle?.set("data_updated", true)
                try {
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    // Fallback navigation
                    findNavController().navigate(R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Gagal mengupdate data: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
    }

    private fun clearForm() {
        binding.radioGroupLayanan.clearCheck()
        binding.radioGroupJenis.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaAkunLayout.editText?.text?.clear()
        binding.alasanLayout.editText?.text?.clear()
        binding.tvFileName.text = getString(R.string.no_file_selected)

        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
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
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)