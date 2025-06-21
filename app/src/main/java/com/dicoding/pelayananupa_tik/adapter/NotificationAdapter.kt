package com.dicoding.pelayananupa_tik.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.NotificationModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    private var onItemClickListener: ((NotificationModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (NotificationModel) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_notification_title)
        private val tvBody: TextView = itemView.findViewById(R.id.tv_notification_body)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_notification_time)

        fun bind(notification: NotificationModel) {
            tvTitle.text = notification.title
            tvBody.text = notification.body
            tvTime.text = formatTimestamp(notification.timestamp)

            // Handle item click
            itemView.setOnClickListener {
                onItemClickListener?.invoke(notification)
            }
        }

        private fun formatTimestamp(timestamp: String): String {
            return try {
                // Parse timestamp format: "June 21, 2025 at 8:29:28 PM UTC+8"
                val inputFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm:ss a z", Locale.ENGLISH)
                val date = inputFormat.parse(timestamp)

                if (date != null) {
                    val now = Date()
                    val diffInMillis = now.time - date.time

                    when {
                        diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "Baru saja"
                        diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                            "$minutes menit lalu"
                        }
                        diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                            "$hours jam lalu"
                        }
                        diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                            "$days hari lalu"
                        }
                        else -> {
                            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                            outputFormat.format(date)
                        }
                    }
                } else {
                    timestamp
                }
            } catch (e: Exception) {
                // Fallback jika parsing gagal
                try {
                    // Coba format alternatif untuk timestamp yang berbeda
                    val simpleFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                    val date = simpleFormat.parse(timestamp)
                    if (date != null) {
                        val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
                        outputFormat.format(date)
                    } else {
                        timestamp
                    }
                } catch (ex: Exception) {
                    timestamp
                }
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem == newItem
        }
    }
}