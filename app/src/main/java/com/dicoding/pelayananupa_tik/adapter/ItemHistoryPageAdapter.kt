package com.dicoding.pelayananupa_tik.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dicoding.pelayananupa_tik.fragment.peminjaman.FinishedItemFragment
import com.dicoding.pelayananupa_tik.fragment.peminjaman.SentItemFragment
import com.dicoding.pelayananupa_tik.fragment.peminjaman.InReviewItemFragment
import com.dicoding.pelayananupa_tik.fragment.peminjaman.TakeItemFragment
import com.dicoding.pelayananupa_tik.fragment.peminjaman.TakenItemFragment

class ItemHistoryPageAdapter (
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle
){
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SentItemFragment()
            1 -> InReviewItemFragment()
            2 -> TakeItemFragment()
            3 -> TakenItemFragment()
            else -> FinishedItemFragment()
        }
    }
}