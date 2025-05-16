package com.dicoding.pelayananupa_tik.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dicoding.pelayananupa_tik.fragment.HistoryLayananFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.AcceptedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.FinishedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.InReviewServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.ProcessedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.RejectedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.SentServiceFragment
import com.dicoding.pelayananupa_tik.fragment.layanan.ServiceHistoryFragment

class ServiceHistoryPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ServiceHistoryFragment()
            1 -> SentServiceFragment()
            2 -> InReviewServiceFragment()
            3 -> AcceptedServiceFragment()
            4 -> ProcessedServiceFragment()
            5 -> RejectedServiceFragment()
            else -> FinishedServiceFragment()
        }
    }
}