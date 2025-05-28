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
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemeliharaanAkunBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        val layanan = view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
        val jenis = if (selectedRadioButtonJenis == R.id.radioOther) {
            binding.editTextOther.text.toString()
        } else {
            view?.findViewById<RadioButton>(selectedRadioButtonJenis)?.text?.toString() ?: ""
        }
        val akun = binding.namaAkunLayout.editText?.text.toString()
        val alasan = binding.alasanLayout.editText?.text.toString()
        val localImagePath = imageUri?.let { saveImageLocally() }

        if (!validateForm(selectedRadioButtonLayanan, selectedRadioButtonJenis, akun, alasan)) return

        saveDataToFirestore(layanan, jenis, akun, alasan, localImagePath)
    }

    private fun validateForm(
        selectedLayanan: Int,
        selectedJenis: Int,
        akun: String,
        alasan: String
    ): Boolean {
        if (selectedLayanan == -1) {
            Toast.makeText(requireContext(), "Harap pilih layanan yang diajukan", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedJenis == -1) {
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

    private fun saveImageLocally(): String? {
        return try {
            val filename = "IMG_${UUID.randomUUID()}.jpg"
            val imagesDir = File(requireContext().filesDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdir()

            val destinationFile = File(imagesDir, filename)

            requireContext().contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun saveDataToFirestore(
        layanan: String,
        jenis: String,
        akun: String,
        alasan: String,
        localImagePath: String?
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
            "localImagePath" to localImagePath,
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