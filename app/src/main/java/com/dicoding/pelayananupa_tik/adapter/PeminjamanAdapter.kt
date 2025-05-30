package com.dicoding.pelayananupa_tik.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.FormPeminjaman

class PeminjamanAdapter(
    private var historyList: List<FormPeminjaman>
) : RecyclerView.Adapter<PeminjamanAdapter.HistoryViewHolder>() {

    companion object {
        private const val TAG = "HistoryAdapter"
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgHistoryItem: ImageView = itemView.findViewById(R.id.img_history_item)
        val tvHistoryName: TextView = itemView.findViewById(R.id.tv_history_name)
        val tvHistoryCategory: TextView = itemView.findViewById(R.id.tv_history_category)
        val tvHistoryStatus: TextView = itemView.findViewById(R.id.tv_history_status)
        val tvHistoryDate: TextView = itemView.findViewById(R.id.tv_history_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        val count = historyList.size
        Log.d(TAG, "getItemCount() returning: $count")
        return count
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        Log.d(TAG, "=== onBindViewHolder called ===")
        Log.d(TAG, "Position: $position")

        if (position >= historyList.size) {
            Log.e(TAG, "Position $position out of bounds for list size ${historyList.size}")
            return
        }

        val history = historyList[position]
        Log.d(TAG, "Binding item: ${history.namaPerangkat}")

        val namaBarang = history.getNamaBarang()
        val jenisBarang = history.getJenisBarang()

        Log.d(TAG, "namaBarang: '$namaBarang'")
        Log.d(TAG, "jenisBarang: '$jenisBarang'")
        Log.d(TAG, "status: '${history.statusPeminjaman}'")
        Log.d(TAG, "tanggal: '${history.tanggalPengajuan}'")

        holder.tvHistoryName.text = namaBarang
        holder.tvHistoryCategory.text = jenisBarang
        holder.tvHistoryStatus.text = history.statusPeminjaman
        holder.tvHistoryDate.text = formatDate(history.tanggalPengajuan)

        // Debug setelah set text
        Log.d(TAG, "TextView values set:")
        Log.d(TAG, "  Name: '${holder.tvHistoryName.text}'")
        Log.d(TAG, "  Category: '${holder.tvHistoryCategory.text}'")
        Log.d(TAG, "  Status: '${holder.tvHistoryStatus.text}'")
        Log.d(TAG, "  Date: '${holder.tvHistoryDate.text}'")

        // Set gambar default
        holder.imgHistoryItem.setImageResource(R.mipmap.ic_launcher)

        // Set warna status
        setStatusColor(holder.tvHistoryStatus, history.statusPeminjaman)

        Log.d(TAG, "=== onBindViewHolder completed ===")
    }

    private fun formatDate(dateString: String): String {
        return if (dateString.isNotEmpty()) {
            try {
                // Sesuaikan format tanggal sesuai yang di Firestore
                dateString.split(" ")[0] // ambil bagian tanggal aja
            } catch (e: Exception) {
                dateString
            }
        } else {
            "Tanggal tidak tersedia"
        }
    }

    private fun setStatusColor(textView: TextView, status: String) {
        val context = textView.context
        when (status.lowercase()) {
            "diajukan" -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            }
            "disetujui" -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            }
            "ditolak" -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
            else -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    fun updateList(newList: List<FormPeminjaman>) {
        Log.d(TAG, "=== UPDATE LIST CALLED ===")
        Log.d(TAG, "Old list size: ${historyList.size}")
        Log.d(TAG, "New list size: ${newList.size}")

        newList.forEachIndexed { index, item ->
            Log.d(TAG, "newList[$index]: namaPerangkat='${item.namaPerangkat}', status='${item.statusPeminjaman}'")
        }

        historyList = newList
        notifyDataSetChanged()

        Log.d(TAG, "notifyDataSetChanged() called")
        Log.d(TAG, "Current historyList size: ${historyList.size}")
    }
}