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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPembuatanWebDllBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPembuatanWebDllFragment : Fragment() {

    private var _binding : FragmentFormPembuatanWebDllBinding? = null
    private val binding get() = _binding!!

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

        binding.radioGroupServices.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

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
        val selectedRadioButtonId = binding.radioGroupServices.checkedRadioButtonId

        if (selectedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Harap pilih layanan yang diajukan", Toast.LENGTH_SHORT).show()
            return
        }

        val layanan = if (selectedRadioButtonId == R.id.radioOther) {
            binding.editTextOther.text.toString()
        } else {
            val radioButton = view?.findViewById<RadioButton>(selectedRadioButtonId)
            radioButton?.text?.toString() ?: ""
        }

        val namaLayanan = binding.namaLayananLayout.editText?.text.toString()
        val kontak = binding.kontakLayout.editText?.text.toString()
        val tujuan = binding.tujuanPembuatanLayout.editText?.text.toString()

        when {
            namaLayanan.isEmpty() -> {
                binding.namaLayananLayout.error = "Nama Layanan tidak boleh kosong"
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
            tujuan.isEmpty() -> {
                binding.tujuanPembuatanLayout.error = "Tujuan Pembuatan tidak boleh kosong"
                return
            }
            else -> {
                binding.namaLayananLayout.error = null
                binding.kontakLayout.error = null
                binding.tujuanPembuatanLayout.error = null

                saveDataToFirestore(layanan, namaLayanan, kontak, tujuan)
            }
        }
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
            "status" to "Draft",
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