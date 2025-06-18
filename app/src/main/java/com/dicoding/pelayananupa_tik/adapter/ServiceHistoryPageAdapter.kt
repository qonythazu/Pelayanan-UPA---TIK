package com.dicoding.pelayananupa_tik.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.AcceptedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.FinishedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.InReviewServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.ProcessedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.RejectedServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.SentServiceFragment
import com.dicoding.pelayananupa_tik.fragment.historyLayanan.DraftServiceFragment

class ServiceHistoryPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DraftServiceFragment()
            1 -> SentServiceFragment()
            2 -> InReviewServiceFragment()
            3 -> AcceptedServiceFragment()
            4 -> ProcessedServiceFragment()
            5 -> RejectedServiceFragment()
            else -> FinishedServiceFragment()
        }
    }
}