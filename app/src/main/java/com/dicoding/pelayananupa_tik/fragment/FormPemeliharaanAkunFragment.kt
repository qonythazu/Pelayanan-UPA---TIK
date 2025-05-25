package com.dicoding.pelayananupa_tik.fragment

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemeliharaanAkunBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FormPemeliharaanAkunFragment : Fragment() {

    private var _binding: FragmentFormPemeliharaanAkunBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

        binding.radioGroupLayanan.setOnCheckedChangeListener { _, _ -> }
        binding.radioGroupJenis.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.btnChooseFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSubmit.setOnClickListener { submitForm() }

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun submitForm() {
        val selectedRadioButtonLayanan = binding.radioGroupLayanan.checkedRadioButtonId
        val selectedRadioButtonJenis = binding.radioGroupJenis.checkedRadioButtonId

        if (selectedRadioButtonLayanan == -1) {
            Toast.makeText(requireContext(), "Harap pilih layanan yang diajukan", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedRadioButtonJenis == -1) {
            Toast.makeText(requireContext(), "Harap pilih jenis pemeliharaan yang diajukan", Toast.LENGTH_SHORT).show()
            return
        }

        val layanan = view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
        val jenis = if (selectedRadioButtonJenis == R.id.radioOther) {
            binding.editTextOther.text.toString()
        } else {
            val radioButton = view?.findViewById<RadioButton>(selectedRadioButtonJenis)
            radioButton?.text?.toString() ?: ""
        }

        val akun = binding.namaAkunLayout.editText?.text.toString()
        val alasan = binding.alasanLayout.editText?.text.toString()

        if (layanan.isEmpty() || jenis.isEmpty() || akun.isEmpty() || alasan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        val localImagePath = if (imageUri != null) {
            saveImageLocally()
        } else {
            null
        }

        saveDataToFirestore(layanan, jenis, akun, alasan, localImagePath)
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

    private fun saveDataToFirestore(layanan: String, jenis: String, akun: String, alasan: String, localImagePath: String?) {
        val userEmail = UserManager.getCurrentUserEmail()
        val pemeliharaan = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Pemeliharaan Akun",
            "layanan" to layanan,
            "jenis" to jenis,
            "akun" to akun,
            "alasan" to alasan,
            "localImagePath" to localImagePath,
            "status" to "Terkirim",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("form_pemeliharaan_akun")
            .add(pemeliharaan)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
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
        imageUri = null
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