package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemasanganPerangkatBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPemasanganPerangkatFragment : Fragment() {

    private var _binding: FragmentFormPemasanganPerangkatBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPemasanganPerangkatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkEditMode()
        firestore = FirebaseFirestore.getInstance()
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
            binding.textView.text = getString(R.string.edit_pemasangan_perangkat)
            binding.btnSubmit.text = getString(R.string.update)
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            val jenis = args.getString("jenis")
            val kontak = args.getString("kontak")
            val tujuan = args.getString("tujuan")

            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    jenis = jenis ?: "",
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
            binding.jenisPerangkatLayout.editText?.setText(item.jenis)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPemasanganLayout.editText?.setText(item.tujuan)
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 10 && phoneNumber.matches(Regex("^[0-9+\\-\\s()]*$"))
    }

    private fun validateForm(formData: Triplet<String, String, String>): Boolean {
        val (jenis, kontak, tujuan) = formData
        var isValid = true
        if (jenis.isBlank()) {
            binding.jenisPerangkatLayout.error = "Jumlah tidak boleh kosong"
            isValid = false
        } else {
            binding.jenisPerangkatLayout.error = null
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
            binding.tujuanPemasanganLayout.error = "Tujuan tidak boleh kosong"
            isValid = false
        } else {
            binding.tujuanPemasanganLayout.error = null
        }

        return isValid
    }

    private fun submitForm() {
        val formData = getFormData()
        if (!validateForm(formData)) return
        saveDataToFirestore(formData.first, formData.second, formData.third)
    }

    private fun updateForm() {
        val formData = getFormData()
        if (!validateForm(formData)) return
        editingItem?.let { item ->
            if (item.documentId.isNotEmpty()) {
                updateDataInFirestore(item.documentId, formData.first, formData.second, formData.third)
            } else {
                Toast.makeText(requireContext(), "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFormData(): Triplet<String, String, String> {
        val jenis = binding.jenisPerangkatLayout.editText?.text.toString().trim()
        val kontak = binding.kontakLayout.editText?.text.toString().trim()
        val tujuan = binding.tujuanPemasanganLayout.editText?.text.toString().trim()

        return Triplet(jenis, kontak, tujuan)
    }

    private fun saveDataToFirestore(jenis: String, kontak: String, tujuan: String) {
        val userEmail = UserManager.getCurrentUserEmail()
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))
        val pemasanganPerangkat = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Pemasangan Perangkat",
            "jenis" to jenis,
            "kontak" to kontak,
            "tujuan" to tujuan,
            "status" to "Draft",
            "timestamp" to formattedDate
        )

        firestore.collection("form_pemasangan")
            .add(pemasanganPerangkat)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
                findNavController().navigate(R.id.action_formPemasanganPerangkatFragment_to_historyLayananFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDataInFirestore(
        documentId: String,
        jenis: String,
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
            "jenis" to jenis,
            "kontak" to kontak,
            "tujuan" to tujuan,
            "lastUpdated" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        firestore.collection("form_pemasangan")
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
                    findNavController().navigate(R.id.action_formBantuanOperatorFragment_to_historyLayananFragment)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Gagal mengupdate data: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = getString(R.string.update)
            }
    }

    private fun clearForm() {
        binding.jenisPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPemasanganLayout.editText?.text?.clear()
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
}
