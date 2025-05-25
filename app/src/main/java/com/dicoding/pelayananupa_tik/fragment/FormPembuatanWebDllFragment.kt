package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPembuatanWebDllBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.firebase.firestore.FirebaseFirestore

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

        if (layanan.isEmpty() || kontak.isEmpty() || tujuan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        saveDataToFirestore(layanan, namaLayanan, kontak, tujuan)
    }

    private fun saveDataToFirestore(layanan: String, namaLayanan: String, kontak: String, tujuan: String) {
        val userEmail = UserManager.getCurrentUserEmail()
        val pembuatanWebDll = hashMapOf(
            "userEmail" to userEmail,
            "judul" to "Form Pembuatan Web/DLL",
            "layanan" to layanan,
            "namaLayanan" to namaLayanan,
            "kontak" to kontak,
            "tujuan" to tujuan,
            "status" to "Terkirim",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("form_pembuatan_web_dll")
            .add(pembuatanWebDll)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                clearForm()
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