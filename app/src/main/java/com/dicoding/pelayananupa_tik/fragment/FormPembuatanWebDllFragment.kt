package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPembuatanWebDllBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPembuatanWebDllFragment : Fragment() {

    private var _binding : FragmentFormPembuatanWebDllBinding? = null
    private val binding get() = _binding!!
    private var isEditMode = false
    private var editingItem: LayananItem? = null
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPembuatanWebDllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        checkEditMode()
        binding.radioGroupServices.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

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
            binding.textView.text = getString(R.string.edit_pembuatan_web_dll)
            binding.btnSubmit.text = getString(R.string.update)
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            val layanan = args.getString("layanan")
            val namaLayanan = args.getString("namaLayanan")
            val kontak = args.getString("kontak")
            val tujuan = args.getString("tujuan")

            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    layanan = layanan ?: "",
                    namaLayanan = namaLayanan ?: "",
                    kontak = kontak ?: "",
                    tujuan = tujuan ?: ""
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
                "Email" to binding.radioEmail
            )
            if (layananRadioButtons.containsKey(item.layanan)) {
                layananRadioButtons[item.layanan]?.isChecked = true
            } else {
                binding.radioOther.isChecked = true
                binding.textInputLayoutOther.visibility = View.VISIBLE
                binding.editTextOther.setText(item.layanan)
            }
            binding.namaLayananLayout.editText?.setText(item.namaLayanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPembuatanLayout.editText?.setText(item.tujuan)

        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10 && phoneNumber.matches(Regex("^[0-9+\\-\\s()]*$"))
    }

    private fun validateForm(formData: Quadruple<String, String, String, String>): Boolean {
        val (layanan, namaLayanan, kontak, tujuan) = formData
        var isValid = true
        if (layanan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap pilih layanan yang diajukan", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (namaLayanan.isBlank()) {
            binding.namaLayananLayout.error = "Nama Layanan tidak boleh kosong"
            isValid = false
        } else {
            binding.namaLayananLayout.error = null
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

        if (tujuan.isBlank()) {
            binding.tujuanPembuatanLayout.error = "Tujuan tidak boleh kosong"
            isValid = false
        } else {
            binding.tujuanPembuatanLayout.error = null
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
        saveDataToFirestore(formData.first, formData.second, formData.third, formData.fourth)
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
                updateDataInFirestore(item.documentId, formData.first, formData.second, formData.third, formData.fourth)
            } else {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
                Toast.makeText(requireContext(), "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            binding.btnSubmit.isEnabled = true
            binding.btnSubmit.text = getString(R.string.update)
            Toast.makeText(requireContext(), "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFormData(): Quadruple<String, String, String, String> {
        val selectedRadioButtonLayanan = binding.radioGroupServices.checkedRadioButtonId
        val layanan = if (selectedRadioButtonLayanan == R.id.radioOther) {
            binding.editTextOther.text.toString().trim()
        } else if (selectedRadioButtonLayanan != -1) {
            view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
        } else ""
        val namaLayanan = binding.namaLayananLayout.editText?.text.toString().trim()
        val kontak = binding.kontakLayout.editText?.text.toString().trim()
        val tujuan = binding.tujuanPembuatanLayout.editText?.text.toString().trim()

        return Quadruple(layanan, namaLayanan, kontak, tujuan)
    }

    private fun saveDataToFirestore(layanan: String, namaLayanan: String, kontak: String, tujuan: String) {
        val userEmail = UserManager.getCurrentUserEmail()
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))
        val pembuatanWebDll = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Pembuatan Web/DLL",
            "layanan" to layanan,
            "namaLayanan" to namaLayanan,
            "kontak" to kontak,
            "tujuan" to tujuan,
            "status" to "draft",
            "timestamp" to formattedDate
        )

        firestore.collection("form_pembuatan")
            .add(pembuatanWebDll)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDataInFirestore(
        documentId: String,
        layanan: String,
        namaLayanan: String,
        kontak: String,
        tujuan: String
    ) {
        if (documentId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Document ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = getString(R.string.updating)

        val updateData = hashMapOf<String, Any>(
            "layanan" to layanan,
            "namaLayanan" to namaLayanan,
            "kontak" to kontak,
            "tujuan" to tujuan,
            "lastUpdated" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        firestore.collection("form_pembuatan")
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
                    findNavController().navigate(R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Gagal mengupdate data: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
    }

    private fun clearForm() {
        binding.radioGroupServices.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaLayananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPembuatanLayout.editText?.text?.clear()
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