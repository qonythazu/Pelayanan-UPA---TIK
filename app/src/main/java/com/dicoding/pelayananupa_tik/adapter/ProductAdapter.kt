package com.dicoding.pelayananupa_tik.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.Barang

class ProductAdapter(
    private var barangList: List<Barang>,
    private val onAddClick: ((Barang) -> Unit)? = null,
    private val showAddButton: Boolean = true
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.img_product)
        val tvName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvCategory: TextView = itemView.findViewById(R.id.tv_product_category)
        val btnAdd: Button = itemView.findViewById(R.id.btn_add)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount() = barangList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val barang = barangList[position]
        holder.tvName.text = barang.namaBarang
        holder.tvCategory.text = barang.jenis
        holder.imgProduct.setImageResource(R.mipmap.ic_launcher)
        if (showAddButton && onAddClick != null) {
            holder.btnAdd.visibility = View.VISIBLE
            holder.btnAdd.setOnClickListener {
                onAddClick.invoke(barang)
            }
        } else {
            holder.btnAdd.visibility = View.GONE
        }
    }

    fun updateList(newList: List<Barang>) {
        barangList = newList
        notifyDataSetChanged()
    }
}