package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            menuPeminjamanBarang.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_productListFragment)
            }
            menuPembuatanWebDll.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPembuatanWebDllFragment)
            }
            menuPemeliharaanAkun.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPemeliharaanAkunFragment)
            }
            menuPengaduanLayanan.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPengaduanLayananFragment)
            }
            menuPemasanganPerangkat.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formPemasanganPerangkatFragment)
            }
            menuBantuanOperatorTik.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_formBantuanOperatorFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
