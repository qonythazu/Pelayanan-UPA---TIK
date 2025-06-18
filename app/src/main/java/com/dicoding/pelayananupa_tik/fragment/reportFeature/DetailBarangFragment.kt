package com.dicoding.pelayananupa_tik.fragment.reportFeature

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.databinding.FragmentDetailBarangBinding

class DetailBarangFragment : Fragment() {

    private var _binding: FragmentDetailBarangBinding? = null
    private val binding get() = _binding!!

    private var namaBarang: String = ""
    private var tanggalMasuk: String = ""
    private var jenisBarang: String = ""
    private var pemilikBarang: String = ""
    private var letakBarang: String = ""
    private var serialNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            namaBarang = args.getString(ARG_NAMA_BARANG)
                ?: args.getString("arg_nama_barang")
                        ?: "Nama tidak tersedia"

            tanggalMasuk = args.getString(ARG_TANGGAL_MASUK)
                ?: args.getString("arg_tanggal_masuk")
                        ?: "Tanggal tidak tersedia"

            jenisBarang = args.getString(ARG_JENIS_BARANG)
                ?: args.getString("arg_jenis_barang")
                        ?: "Jenis tidak tersedia"

            pemilikBarang = args.getString(ARG_PEMILIK_BARANG)
                ?: args.getString("arg_pemilik_barang")
                        ?: "Pemilik tidak tersedia"

            letakBarang = args.getString(ARG_LETAK_BARANG)
                ?: args.getString("arg_letak_barang")
                        ?: "Lokasi tidak tersedia"

            serialNumber = args.getString(ARG_SERIAL_NUMBER)
                ?: args.getString("arg_serial_number")
                        ?: "Serial tidak tersedia"

            // Log untuk debugging
            Log.d(TAG, "Data received - Nama: $namaBarang, Jenis: $jenisBarang, Serial: $serialNumber")
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

        setupViews()
        setupUI()
        setupLaporKerusakanButton()

        Log.d(TAG, "UI setup completed with data")
    }

    private fun setupViews() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupUI() {
        try {
            binding.namaBarang.text = namaBarang.ifEmpty { "Nama tidak tersedia" }
            binding.tanggalMasukEditText.setText(tanggalMasuk)
            binding.jenisBarangEditText.setText(jenisBarang)
            binding.pemilikBarangEditText.setText(pemilikBarang)
            binding.letakBarangEditText.setText(letakBarang)
            binding.serialNumberEditText.setText(serialNumber)

            // Log untuk memastikan data terisi
            Log.d(TAG, "UI updated with:")
            Log.d(TAG, "- Nama: $namaBarang")
            Log.d(TAG, "- Tanggal: $tanggalMasuk")
            Log.d(TAG, "- Jenis: $jenisBarang")
            Log.d(TAG, "- Pemilik: $pemilikBarang")
            Log.d(TAG, "- Letak: $letakBarang")
            Log.d(TAG, "- Serial: $serialNumber")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up UI", e)
        }
    }

    private fun setupLaporKerusakanButton() {
        try {
            // Perbaikan: Setup tombol lapor kerusakan dengan view binding
            binding.btnLaporKerusakan.setOnClickListener {
                navigateToLaporKerusakan()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up lapor kerusakan button", e)
        }
    }

    private fun navigateToLaporKerusakan() {
        try {
            val bundle = Bundle().apply {
                putString("nama_perangkat", namaBarang)
                putString("serial_number", serialNumber)
            }

            val navController = requireActivity().findNavController(R.id.nav_home_fragment)
            navController.navigate(R.id.formLaporKerusakanFragment, bundle)

            Log.d(TAG, "Navigation to FormLaporKerusakanFragment successful")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to FormLaporKerusakanFragment", e)
            Toast.makeText(requireContext(), "Gagal membuka form lapor kerusakan", Toast.LENGTH_SHORT).show()
        }
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
        private const val TAG = "DetailBarangFragment"
        private const val ARG_NAMA_BARANG = "arg_nama_barang"
        private const val ARG_TANGGAL_MASUK = "arg_tanggal_masuk"
        private const val ARG_JENIS_BARANG = "arg_jenis_barang"
        private const val ARG_PEMILIK_BARANG = "arg_pemilik_barang"
        private const val ARG_LETAK_BARANG = "arg_letak_barang"
        private const val ARG_SERIAL_NUMBER = "arg_serial_number"

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

        fun newInstanceFromBundle(bundle: Bundle): DetailBarangFragment {
            val fragment = DetailBarangFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}