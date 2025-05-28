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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemasanganPerangkatBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormPemasanganPerangkatFragment : Fragment() {

    private var _binding: FragmentFormPemasanganPerangkatBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var jenisPerangkatInput: TextInputEditText
    private lateinit var kontakInput: TextInputEditText
    private lateinit var tujuanPemasanganInput: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPemasanganPerangkatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        val jenisPerangkatLayout = view.findViewById<TextInputLayout>(R.id.jenisPerangkatLayout)
        jenisPerangkatInput = jenisPerangkatLayout.editText as TextInputEditText

        val kontakLayout = view.findViewById<TextInputLayout>(R.id.kontakLayout)
        kontakInput = kontakLayout.editText as TextInputEditText

        val tujuanPemasanganLayout = view.findViewById<TextInputLayout>(R.id.tujuanPemasanganLayout)
        tujuanPemasanganInput = tujuanPemasanganLayout.editText as TextInputEditText

        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnSubmit.setOnClickListener { submitForm() }

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
        val jenis = binding.jenisPerangkatLayout.editText?.text.toString()
        val kontak = binding.kontakLayout.editText?.text.toString()
        val tujuan = binding.tujuanPemasanganLayout.editText?.text.toString()

        when {
            jenis.isEmpty() -> {
                binding.jenisPerangkatLayout.error = "Jenis Perangkat tidak boleh kosong"
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
                binding.tujuanPemasanganLayout.error = "Tujuan Pemasangan tidak boleh kosong"
                return
            }
            else -> {
                binding.jenisPerangkatLayout.error = null
                binding.kontakLayout.error = null
                binding.tujuanPemasanganLayout.error = null

                saveDataToFirestore(jenis, kontak, tujuan)
            }
        }
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

    private fun clearForm() {
        jenisPerangkatInput.text?.clear()
        kontakInput.text?.clear()
        tujuanPemasanganInput.text?.clear()
        jenisPerangkatInput.clearFocus()
        kontakInput.clearFocus()
        tujuanPemasanganInput.clearFocus()
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
