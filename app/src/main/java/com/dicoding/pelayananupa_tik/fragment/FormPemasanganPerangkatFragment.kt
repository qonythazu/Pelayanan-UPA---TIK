package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class FormPemasanganPerangkatFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var jenisPerangkatInput: TextInputEditText
    private lateinit var kontakInput: TextInputEditText
    private lateinit var tujuanPemasanganInput: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_form_pemasangan_perangkat, container, false)

        firestore = FirebaseFirestore.getInstance()

        val jenisPerangkatLayout = view.findViewById<TextInputLayout>(R.id.jenisPerangkatLayout)
        jenisPerangkatInput = jenisPerangkatLayout.editText as TextInputEditText

        val kontakLayout = view.findViewById<TextInputLayout>(R.id.kontakLayout)
        kontakInput = kontakLayout.editText as TextInputEditText

        val tujuanPemasanganLayout = view.findViewById<TextInputLayout>(R.id.tujuanPemasanganLayout)
        tujuanPemasanganInput = tujuanPemasanganLayout.editText as TextInputEditText

        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnSubmit.setOnClickListener { submitForm() }

        return view
    }

    private fun submitForm() {
        val jenisPerangkat = jenisPerangkatInput.text?.toString()?.trim() ?: ""
        val kontak = kontakInput.text?.toString()?.trim() ?: ""
        val tujuanPemasangan = tujuanPemasanganInput.text?.toString()?.trim() ?: ""

        if (jenisPerangkat.isEmpty() || kontak.isEmpty() || tujuanPemasangan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
            return
        }

        val formData = hashMapOf(
            "jenis_perangkat" to jenisPerangkat,
            "kontak_penanggung_jawab" to kontak,
            "tujuan_pemasangan" to tujuanPemasangan,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("form_pemasangan")
            .add(formData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Form berhasil dikirim: ${documentReference.id}")
                Toast.makeText(requireContext(), "Form berhasil dikirim!", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal mengirim form", e)
                Toast.makeText(requireContext(), "Gagal mengirim form", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
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
