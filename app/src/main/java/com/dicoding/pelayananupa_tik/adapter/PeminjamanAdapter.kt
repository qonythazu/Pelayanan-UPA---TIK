package com.dicoding.pelayananupa_tik.adapter

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
    private var historyList: List<Pair<String, FormPeminjaman>>, // Pair<DocumentId, FormPeminjaman>
    private val onTakenClick: ((String, FormPeminjaman) -> Unit)? = null,
    private val onReturnedClick: ((String, FormPeminjaman) -> Unit)? = null
) : RecyclerView.Adapter<PeminjamanAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgHistoryItem: ImageView = itemView.findViewById(R.id.img_history_item)
        val tvHistoryName: TextView = itemView.findViewById(R.id.tv_history_name)
        val tvHistoryCategory: TextView = itemView.findViewById(R.id.tv_history_category)
        val tvHistoryStatus: TextView = itemView.findViewById(R.id.tv_history_status)
        val tvHistoryDate: TextView = itemView.findViewById(R.id.tv_history_date)
        val btnTaken: TextView = itemView.findViewById(R.id.btnTaken)
        val btnReturned: TextView = itemView.findViewById(R.id.btnReturned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int = historyList.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        if (position >= historyList.size) return

        val (documentId, history) = historyList[position]
        val namaBarang = history.getNamaBarang()
        val jenisBarang = history.getJenisBarang()

        holder.tvHistoryName.text = namaBarang
        holder.tvHistoryCategory.text = jenisBarang
        holder.tvHistoryStatus.text = history.statusPeminjaman
        holder.tvHistoryDate.text = formatDate(history.tanggalPengajuan)

        // Set gambar default
        holder.imgHistoryItem.setImageResource(R.mipmap.ic_launcher)

        // Set warna status
        setStatusColor(holder.tvHistoryStatus, history.statusPeminjaman)

        // Handle button visibility dan click berdasarkan status
        setupButtons(holder, documentId, history)
    }

    private fun setupButtons(holder: HistoryViewHolder, documentId: String, history: FormPeminjaman) {
        // Reset visibility - semua button disembunyikan dulu
        holder.btnTaken.visibility = View.GONE
        holder.btnReturned.visibility = View.GONE

        when (history.statusPeminjaman.lowercase()) {
            "disetujui" -> {
                // Tampilkan button "Taken" jika status "Disetujui"
                holder.btnTaken.visibility = View.VISIBLE
                holder.btnTaken.setOnClickListener {
                    onTakenClick?.invoke(documentId, history)
                }
            }
            "diambil" -> {
                // Tampilkan button "Returned" jika status "Diambil"
                holder.btnReturned.visibility = View.VISIBLE
                holder.btnReturned.setOnClickListener {
                    onReturnedClick?.invoke(documentId, history)
                }
            }
            // Status lain seperti "Diajukan", "Ditolak", "Selesai" tidak menampilkan button apapun
        }
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
            "diambil" -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            }
            "selesai" -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            }
            else -> {
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    fun updateList(newList: List<Pair<String, FormPeminjaman>>) {
        historyList = newList
        notifyDataSetChanged()
    }
}