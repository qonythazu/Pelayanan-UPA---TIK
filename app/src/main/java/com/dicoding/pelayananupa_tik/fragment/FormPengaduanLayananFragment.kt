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
        _binding = FragmentFormPengaduanLayananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        binding.btnChooseFile.setOnClickListener { openPdfPicker() }
        binding.btnSubmit.setOnClickListener { submitForm() }

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10 && phoneNumber.matches(Regex("^[0-9+\\-\\s()]*$"))
    }

    private fun submitForm() {
        val layanan = binding.layananLayout.editText?.text.toString()
        val kontak = binding.kontakLayout.editText?.text.toString()
        val keluhan = binding.keluhanAndaLayout.editText?.text.toString()

        when {
            layanan.isEmpty() -> {
                binding.layananLayout.error = "Layanan tidak boleh kosong"
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
            keluhan.isEmpty() -> {
                binding.keluhanAndaLayout.error = "Keluhan tidak boleh kosong"
                return
            }
            else -> {
                binding.layananLayout.error = null
                binding.kontakLayout.error = null
                binding.keluhanAndaLayout.error = null

                saveDataToFirestore(layanan, kontak, keluhan)
            }
        }
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
            "status" to "Draft",
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
}