package com.dicoding.pelayananupa_tik.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.Barang

class BoxAdapter(
    private var boxItems: MutableList<Barang>,
    private val onItemChecked: (Barang, Boolean) -> Unit
) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    private val selectedItems = mutableListOf<Barang>()
    private var recyclerView: RecyclerView? = null

    inner class BoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkItem: CheckBox = itemView.findViewById(R.id.check_item)
        val imgProduct: ImageView = itemView.findViewById(R.id.img_product)
        val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvProductCategory: TextView = itemView.findViewById(R.id.tv_product_category)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row_box, parent, false)
        return BoxViewHolder(view)
    }

    override fun getItemCount() = boxItems.size

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        val barang = boxItems[position]

        holder.tvProductName.text = barang.namaBarang
        holder.tvProductCategory.text = barang.jenis

        // Load image menggunakan Glide
        loadImage(holder.imgProduct, barang.photoUrl)

        holder.checkItem.isChecked = selectedItems.any { it.namaBarang == barang.namaBarang }
        holder.checkItem.setOnCheckedChangeListener(null) // Clear previous listener
        holder.checkItem.setOnCheckedChangeListener { _, isChecked ->
            recyclerView?.post {
                onItemChecked(barang, isChecked)

                if (isChecked) {
                    if (!selectedItems.any { it.namaBarang == barang.namaBarang }) {
                        selectedItems.add(barang)
                    }
                } else {
                    selectedItems.removeAll { it.namaBarang == barang.namaBarang }
                }
            }
        }

        holder.itemView.setOnClickListener {
            holder.checkItem.isChecked = !holder.checkItem.isChecked
        }
    }

    private fun loadImage(imageView: ImageView, photoUrl: String) {
        if (photoUrl.isNotEmpty()) {
            Glide.with(imageView.context)
                .load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.mipmap.ic_launcher) // Gambar sementara saat loading
                .error(R.mipmap.ic_launcher) // Gambar jika gagal load
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(imageView)
        } else {
            // Jika photoUrl kosong, gunakan gambar default
            imageView.setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun updateList(newList: MutableList<Barang>) {
        boxItems = newList
        safeNotifyDataSetChanged()
    }

    fun updateSelectedItems(newSelectedItems: List<Barang>) {
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        safeNotifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(boxItems)
        safeNotifyDataSetChanged()
    }

    fun unselectAll() {
        selectedItems.clear()
        safeNotifyDataSetChanged()
    }

    // Helper method untuk menghindari crash saat RecyclerView sedang layout/scroll
    private fun safeNotifyDataSetChanged() {
        val rv = recyclerView
        if (rv != null && !rv.isComputingLayout) {
            notifyDataSetChanged()
        } else {
            Handler(Looper.getMainLooper()).post {
                notifyDataSetChanged()
            }
        }
    }
}