package com.dicoding.pelayananupa_tik.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.Barang

class BoxAdapter(
    private var boxItems: MutableList<Barang>,
    private val onItemChecked: (Barang, Boolean) -> Unit
) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    private val selectedItems = mutableListOf<Barang>()

    inner class BoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkItem: CheckBox = itemView.findViewById(R.id.check_item)
        val imgProduct: ImageView = itemView.findViewById(R.id.img_product)
        val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvProductCategory: TextView = itemView.findViewById(R.id.tv_product_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row_box, parent, false)
        return BoxViewHolder(view)
    }

    override fun getItemCount() = boxItems.size

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        val barang = boxItems[position]

        // Set data
        holder.tvProductName.text = barang.namaBarang
        holder.tvProductCategory.text = barang.jenis
        holder.imgProduct.setImageResource(R.mipmap.ic_launcher)

        // Set checkbox state
        holder.checkItem.isChecked = selectedItems.any { it.namaBarang == barang.namaBarang }

        // Handle checkbox click
        holder.checkItem.setOnCheckedChangeListener(null) // Clear previous listener
        holder.checkItem.setOnCheckedChangeListener { _, isChecked ->
            onItemChecked(barang, isChecked)

            if (isChecked) {
                if (!selectedItems.any { it.namaBarang == barang.namaBarang }) {
                    selectedItems.add(barang)
                }
            } else {
                selectedItems.removeAll { it.namaBarang == barang.namaBarang }
            }
        }

        // Handle item click to toggle checkbox
        holder.itemView.setOnClickListener {
            holder.checkItem.isChecked = !holder.checkItem.isChecked
        }
    }

    fun updateList(newList: MutableList<Barang>) {
        boxItems = newList
        notifyDataSetChanged()
    }

    fun updateSelectedItems(newSelectedItems: List<Barang>) {
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(boxItems)
        notifyDataSetChanged()
    }

    fun unselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
    }
}