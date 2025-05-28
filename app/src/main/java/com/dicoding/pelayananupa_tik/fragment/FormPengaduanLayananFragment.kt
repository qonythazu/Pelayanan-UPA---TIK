package com.dicoding.pelayananupa_tik.fragment

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
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
import java.util.UUID

class FormPengaduanLayananFragment : Fragment() {

    private var _binding: FragmentFormPengaduanLayananBinding? = null
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
        _binding = FragmentFormPengaduanLayananBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        binding.btnChooseFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSubmit.setOnClickListener { submitForm() }

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
        val localImagePath = if (imageUri != null) {
            saveImageLocally()
        } else {
            null
        }

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

                saveDataToFirestore(layanan, kontak, keluhan, localImagePath)
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

    private fun saveDataToFirestore(layanan: String, kontak: String, keluhan: String, localImagePath: String?) {
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
            "localImagePath" to localImagePath,
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