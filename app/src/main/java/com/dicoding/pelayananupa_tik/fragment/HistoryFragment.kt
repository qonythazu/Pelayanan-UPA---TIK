package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val btnHistoryPeminjamanBarang = view.findViewById<LinearLayout>(R.id.btn_history_item)
        val btnHistoryLayanan = view.findViewById<LinearLayout>(R.id.btn_history_service)

        btnHistoryPeminjamanBarang.setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_historyPeminjamanBarangFragment)
        }

        btnHistoryLayanan.setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_historyLayananFragment)
        }

        return view
    }
}