package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentDetailBarangBinding

class DetailBarangFragment : Fragment() {

    private var _binding: FragmentDetailBarangBinding? = null
    private val binding get() = _binding!!

    // Data properties
    private var namaBarang: String = ""
    private var tanggalMasuk: String = ""
    private var jenisBarang: String = ""
    private var pemilikBarang: String = ""
    private var letakBarang: String = ""
    private var serialNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            namaBarang = it.getString(ARG_NAMA_BARANG, "")
            tanggalMasuk = it.getString(ARG_TANGGAL_MASUK, "")
            jenisBarang = it.getString(ARG_JENIS_BARANG, "")
            pemilikBarang = it.getString(ARG_PEMILIK_BARANG, "")
            letakBarang = it.getString(ARG_LETAK_BARANG, "")
            serialNumber = it.getString(ARG_SERIAL_NUMBER, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBarangBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Langsung pakai properti yang sudah diisi di onCreate()
        binding.namaBarang.text = namaBarang
        binding.tanggalMasukEditText.setText(tanggalMasuk)
        binding.jenisBarangEditText.setText(jenisBarang)
        binding.pemilikBarangEditText.setText(pemilikBarang)
        binding.letakBarangEditText.setText(letakBarang)
        binding.serialNumberEditText.setText(serialNumber)
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

    companion object {
        private const val ARG_NAMA_BARANG = "arg_nama_barang"
        private const val ARG_TANGGAL_MASUK = "arg_tanggal_masuk"
        private const val ARG_JENIS_BARANG = "arg_jenis_barang"
        private const val ARG_PEMILIK_BARANG = "arg_pemilik_barang"
        private const val ARG_LETAK_BARANG = "arg_letak_barang"
        private const val ARG_SERIAL_NUMBER = "arg_serial_number"

        // Factory method
        fun newInstance(
            namaBarang: String,
            tanggalMasuk: String,
            jenisBarang: String,
            pemilikBarang: String,
            letakBarang: String,
            serialNumber: String
        ): DetailBarangFragment {
            val fragment = DetailBarangFragment()
            val args = Bundle().apply {
                putString(ARG_NAMA_BARANG, namaBarang)
                putString(ARG_TANGGAL_MASUK, tanggalMasuk)
                putString(ARG_JENIS_BARANG, jenisBarang)
                putString(ARG_PEMILIK_BARANG, pemilikBarang)
                putString(ARG_LETAK_BARANG, letakBarang)
                putString(ARG_SERIAL_NUMBER, serialNumber)
            }
            fragment.arguments = args
            return fragment
        }
    }
}