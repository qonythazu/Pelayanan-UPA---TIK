package com.dicoding.pelayananupa_tik.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPengaduanLayananBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class FormPengaduanLayananFragment : Fragment() {

    private var _binding: FragmentFormPengaduanLayananBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        binding.tvFileName.text = uri?.lastPathSegment ?: "File selected"
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
        storage = FirebaseStorage.getInstance()

        binding.btnChooseFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSubmit.setOnClickListener { submitForm() }
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

    private fun submitForm() {
        val layanan = binding.layananLayout.editText?.text.toString()
        val kontak = binding.kontakLayout.editText?.text.toString()
        val keluhan = binding.keluhanAndaLayout.editText?.text.toString()

        if (layanan.isEmpty() || kontak.isEmpty() || keluhan.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            uploadImageAndSaveData(layanan, kontak, keluhan)
        } else {
            saveDataToFirestore(layanan, kontak, keluhan, null)
        }
    }

    private fun uploadImageAndSaveData(layanan: String, kontak: String, keluhan: String) {
        val fileName = "images/${UUID.randomUUID()}"
        val storageRef = storage.reference.child(fileName)

        imageUri?.let {
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveDataToFirestore(layanan, kontak, keluhan, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal mengupload gambar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveDataToFirestore(layanan: String, kontak: String, keluhan: String, imageUrl: String?) {
        val pengaduan = hashMapOf(
            "layanan" to layanan,
            "kontak" to kontak,
            "keluhan" to keluhan,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("pengaduan")
            .add(pengaduan)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
