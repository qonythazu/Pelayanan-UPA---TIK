package com.dicoding.pelayananupa_tik.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.LayananItem

class LayananAdapter(
    private val layananList: MutableList<LayananItem>,
    private val onEditItem: (LayananItem, Int) -> Unit = { _, _ -> },
    private val onStatusChanged: (LayananItem, Int) -> Unit = { _, _ -> },
    private val onDeleteItem: (LayananItem, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<LayananAdapter.LayananViewHolder>() {

    class LayananViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layananText: TextView = itemView.findViewById(R.id.textLayanan)
        val tanggalText: TextView = itemView.findViewById(R.id.textTanggal)
        val statusText: TextView = itemView.findViewById(R.id.textStatus)
        val btnMore: ImageView = itemView.findViewById(R.id.btn_more)
        val btnBatalkan: TextView = itemView.findViewById(R.id.btnBatalkan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayananViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layanan, parent, false)
        return LayananViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayananViewHolder, position: Int) {
        val context = holder.itemView.context
        val layananItem = layananList[position]

        holder.layananText.text = layananItem.judul
        holder.tanggalText.text = layananItem.tanggal
        holder.statusText.text = layananItem.status

        when (layananItem.status.lowercase()) {
            "selesai" -> holder.statusText.setTextColor(Color.parseColor("#34C759")) // Green
            "ditolak" -> holder.statusText.setTextColor(Color.parseColor("#FF3B30")) // Red
            else -> holder.statusText.setTextColor(Color.parseColor("#0067AC"))       // Blue
        }

        // Handle button visibility based on status
        when (layananItem.status.lowercase()) {
            "draft" -> {
                holder.btnMore.visibility = View.VISIBLE
                holder.btnBatalkan.visibility = View.VISIBLE
                holder.btnBatalkan.text = context.getString(R.string.submit)
                holder.btnBatalkan.setTextColor(ContextCompat.getColor(context, R.color.green))
                setupMoreButton(holder, context, layananItem, position)
                setupSubmitButton(holder, context, layananItem, position)
            }
            "terkirim" -> {
                holder.btnMore.visibility = View.GONE
                holder.btnBatalkan.visibility = View.VISIBLE
                holder.btnBatalkan.text = context.getString(R.string.batalkan)
                holder.btnBatalkan.setTextColor(ContextCompat.getColor(context, R.color.red))
                setupBatalkanButton(holder, context, layananItem, position)
            }
            else -> {
                holder.btnMore.visibility = View.GONE
                holder.btnBatalkan.visibility = View.GONE
            }
        }
    }

    private fun setupMoreButton(holder: LayananViewHolder, context: Context, layananItem: LayananItem, position: Int) {
        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(context, view, Gravity.END, 0, R.style.CustomPopupMenu)
            popup.menuInflater.inflate(R.menu.item_menu, popup.menu)

            val updateItem = popup.menu.findItem(R.id.action_update)
            val deleteItem = popup.menu.findItem(R.id.action_delete)

            val green = ContextCompat.getColor(context, R.color.green)
            val red = ContextCompat.getColor(context, R.color.red)

            updateItem.icon?.setTint(green)
            deleteItem.icon?.setTint(red)

            updateItem.title = buildColoredText("Update", green)
            deleteItem.title = buildColoredText("Delete", red)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_update -> {
                        navigateToEditForm(view, layananItem)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog(context, layananItem, position)
                        true
                    }
                    else -> false
                }
            }
            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popup)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val scale = context.resources.displayMetrics.density
            val offsetY = (8 * scale + 0.5f).toInt()
            popup.show()
            try {
                val mPopupField = popup.javaClass.getDeclaredField("mPopup")
                mPopupField.isAccessible = true
                val mPopup = mPopupField.get(popup)
                mPopup.javaClass
                    .getMethod("show", Int::class.java, Int::class.java)
                    .invoke(mPopup, 0, offsetY)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigateToEditForm(view: View, layananItem: LayananItem) {
        val bundle = Bundle().apply {
            putString("documentId", layananItem.documentId)
            putString("layanan", layananItem.layanan)
            putString("jenis", layananItem.jenis)
            putString("akun", layananItem.akun)
            putString("alasan", layananItem.alasan)
            putString("jumlah", layananItem.jumlah)
            putString("kontak", layananItem.kontak)
            putString("tujuan", layananItem.tujuan)
            putString("filePath", layananItem.filePath)
            putBoolean("isEditMode", true)
        }
        val navigationId = when (layananItem.formType) {
            "pemeliharaan_akun" -> R.id.action_historyLayananFragment_to_formPemeliharaanAkunFragment
            "bantuan" -> R.id.action_historyLayananFragment_to_formBantuanOperatorFragment
            "pemasangan" -> R.id.action_historyLayananFragment_to_formPemasanganPerangkatFragment
            else -> R.id.action_historyLayananFragment_to_formPemeliharaanAkunFragment // default
        }

        try {
            view.findNavController().navigate(navigationId, bundle)
        } catch (e: Exception) {
            println("Navigation failed: ${e.message}")
            e.printStackTrace()
            onEditItem(layananItem, layananList.indexOf(layananItem))
        }
    }

    private fun setupSubmitButton(holder: LayananViewHolder, context: Context, layananItem: LayananItem, position: Int) {
        holder.btnBatalkan.setOnClickListener {
            val updatedItem = layananItem.copy(status = "Terkirim")
            layananList[position] = updatedItem
            notifyItemChanged(position)
            onStatusChanged(updatedItem, position)
            Toast.makeText(context, "Data berhasil dikirim", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBatalkanButton(holder: LayananViewHolder, context: Context, layananItem: LayananItem, position: Int) {
        holder.btnBatalkan.setOnClickListener {
            showCancelConfirmationDialog(context, layananItem, position)
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, layananItem: LayananItem, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus layanan \"${layananItem.judul}\"?")
            .setPositiveButton("Ya") { dialog, _ ->
                layananList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, layananList.size)

                Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onDeleteItem(layananItem, position)
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun showCancelConfirmationDialog(context: Context, layananItem: LayananItem, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Pembatalan")
            .setMessage("Apakah Anda yakin ingin membatalkan layanan \"${layananItem.judul}\"?")
            .setPositiveButton("Ya") { dialog, _ ->
                layananList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, layananList.size)

                Toast.makeText(context, "Layanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    override fun getItemCount(): Int = layananList.size

    private fun buildColoredText(text: String, color: Int): CharSequence {
        val spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    fun removeItem(position: Int) {
        if (position in 0 until layananList.size) {
            layananList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, layananList.size)
        }
    }

    fun resetSubmitButton(position: Int) {
        if (position in 0 until layananList.size) {
            notifyItemChanged(position)
        }
    }
}