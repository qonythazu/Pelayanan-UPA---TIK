package com.dicoding.pelayananupa_tik.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dicoding.pelayananupa_tik.fragment.historyPeminjaman.FinishedItemFragment
import com.dicoding.pelayananupa_tik.fragment.historyPeminjaman.SentItemFragment
import com.dicoding.pelayananupa_tik.fragment.historyPeminjaman.ApprovedItemFragment
import com.dicoding.pelayananupa_tik.fragment.historyPeminjaman.TakenItemFragment
import com.dicoding.pelayananupa_tik.fragment.historyPeminjaman.RejectedItemFragment

class ItemHistoryPageAdapter (
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle
){
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SentItemFragment()
            1 -> ApprovedItemFragment()
            2 -> TakenItemFragment()
            3 -> RejectedItemFragment()
            else -> FinishedItemFragment()
        }
    }
}