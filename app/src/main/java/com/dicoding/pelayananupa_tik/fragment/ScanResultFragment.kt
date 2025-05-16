package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.FragmentScanResultBinding

class ScanResultFragment : Fragment(), ScanFragment.ScanResultListener {

    private var _binding: FragmentScanResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup scan button
        binding.btnScan.setOnClickListener {
            // Tampilkan ScanFragment sebagai BottomSheet dan daftarkan listener
            ScanFragment.show(parentFragmentManager, this)
        }
    }

    // Implementasi ScanResultListener
    override fun onScanResult(
        nama: String,
        tanggalMasuk: String,
        jenisBarang: String,
        pemilikBarang: String,
        letakBarang: String,
        serialNumber: String
    ) {
        Log.d("ScanResultFragment", "Scan result received: $nama, $serialNumber")

        // Buat DetailBarangFragment menggunakan factory method
        val detailFragment = DetailBarangFragment.newInstance(
            nama,
            tanggalMasuk,
            jenisBarang,
            pemilikBarang,
            letakBarang,
            serialNumber
        )

        // Ganti fragment saat ini dengan DetailBarangFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.detailBarangFragment, detailFragment)
            .addToBackStack(null)
            .commit()

        // Alternatif menggunakan Navigation Component jika Anda menggunakannya
        // val bundle = Bundle().apply {
        //     putString("arg_nama_barang", nama)
        //     putString("arg_tanggal_masuk", tanggalMasuk)
        //     putString("arg_jenis_barang", jenisBarang)
        //     putString("arg_pemilik_barang", pemilikBarang)
        //     putString("arg_letak_barang", letakBarang)
        //     putString("arg_serial_number", serialNumber)
        // }
        // findNavController().navigate(R.id.action_scanResultFragment_to_detailBarangFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}