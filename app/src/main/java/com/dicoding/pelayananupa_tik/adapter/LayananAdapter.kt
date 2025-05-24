package com.dicoding.pelayananupa_tik.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R

class LayananAdapter(private val layananList: List<String>) :
    RecyclerView.Adapter<LayananAdapter.LayananViewHolder>() {

    class LayananViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layananText: TextView = view.findViewById(R.id.textLayanan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayananViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layanan, parent, false)
        return LayananViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayananViewHolder, position: Int) {
        holder.layananText.text = layananList[position]
    }

    override fun getItemCount(): Int = layananList.size
}
