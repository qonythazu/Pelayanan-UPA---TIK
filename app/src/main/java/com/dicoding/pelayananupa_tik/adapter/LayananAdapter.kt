package com.dicoding.pelayananupa_tik.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
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

        bindBasicInfo(holder, layananItem)
        setStatusColor(holder, layananItem)
        setupButtonsBasedOnStatus(holder, context, layananItem, position)
    }

    private fun bindBasicInfo(holder: LayananViewHolder, layananItem: LayananItem) {
        holder.layananText.text = layananItem.judul
        holder.tanggalText.text = layananItem.tanggal
        holder.statusText.text = layananItem.status
    }

    private fun setStatusColor(holder: LayananViewHolder, layananItem: LayananItem) {
        val color = when (layananItem.status.lowercase()) {
            "selesai" -> Color.parseColor("#34C759") // Green
            "ditolak" -> Color.parseColor("#FF3B30") // Red
            else -> Color.parseColor("#0067AC")       // Blue
        }
        holder.statusText.setTextColor(color)
    }

    private fun setupButtonsBasedOnStatus(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        when (layananItem.status.lowercase()) {
            "draft" -> setupDraftButtons(holder, context, layananItem, position)
            "terkirim" -> setupTerkirimButtons(holder, context, layananItem, position)
            else -> hideAllButtons(holder)
        }
    }

    private fun setupDraftButtons(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        holder.btnMore.visibility = View.VISIBLE
        holder.btnBatalkan.visibility = View.VISIBLE
        holder.btnBatalkan.text = context.getString(R.string.submit)
        holder.btnBatalkan.setTextColor(ContextCompat.getColor(context, R.color.green))

        setupMoreButton(holder, context, layananItem, position)
        setupSubmitButton(holder, context, layananItem, position)
    }

    private fun setupTerkirimButtons(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        holder.btnMore.visibility = View.GONE
        holder.btnBatalkan.visibility = View.VISIBLE
        holder.btnBatalkan.text = context.getString(R.string.batalkan)
        holder.btnBatalkan.setTextColor(ContextCompat.getColor(context, R.color.red))

        setupBatalkanButton(holder, context, layananItem, position)
    }

    private fun hideAllButtons(holder: LayananViewHolder) {
        holder.btnMore.visibility = View.GONE
        holder.btnBatalkan.visibility = View.GONE
    }

    private fun setupMoreButton(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        holder.btnMore.setOnClickListener { view ->
            showPopupMenu(view, context, layananItem, position)
        }
    }

    private fun showPopupMenu(view: View, context: Context, layananItem: LayananItem, position: Int) {
        val popup = PopupMenu(context, view, Gravity.END, 0, R.style.CustomPopupMenu)
        popup.menuInflater.inflate(R.menu.item_menu, popup.menu)

        stylePopupMenu(popup, context)
        setPopupMenuListener(popup, view, layananItem, position, context)
        popup.show()
    }

    private fun stylePopupMenu(popup: PopupMenu, context: Context) {
        val updateItem = popup.menu.findItem(R.id.action_update)
        val deleteItem = popup.menu.findItem(R.id.action_delete)

        val green = ContextCompat.getColor(context, R.color.green)
        val red = ContextCompat.getColor(context, R.color.red)

        updateItem.icon?.setTint(green)
        deleteItem.icon?.setTint(red)

        updateItem.title = buildColoredText("Update", green)
        deleteItem.title = buildColoredText("Delete", red)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }
    }

    private fun setPopupMenuListener(
        popup: PopupMenu,
        view: View,
        layananItem: LayananItem,
        position: Int,
        context: Context
    ) {
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
    }

    private fun navigateToEditForm(view: View, layananItem: LayananItem) {
        val bundle = createEditBundle(layananItem)
        val navigationId = getNavigationId(layananItem.formType)

        if (navigationId == null) {
            handleUnknownFormType(view, layananItem)
            return
        }

        try {
            view.findNavController().navigate(navigationId, bundle)
        } catch (e: Exception) {
            handleNavigationError(e, layananItem)
        }
    }

    private fun createEditBundle(layananItem: LayananItem): Bundle {
        Log.d("LayananAdapter", "Creating bundle for formType: ${layananItem.formType}")
        Log.d("LayananAdapter", "LayananItem data: $layananItem")

        val bundle = Bundle().apply {
            putString("documentId", layananItem.documentId)
            putBoolean("isEditMode", true)
        }

        when (layananItem.formType) {
            "pemeliharaan" -> bundle.apply {
                putString("layanan", layananItem.layanan)
                putString("jenis", layananItem.jenis)
                putString("akun", layananItem.akun)
                putString("alasan", layananItem.alasan)
                putString("filePath", layananItem.filePath)
            }

            "bantuan" -> bundle.apply {
                putString("jumlah", layananItem.jumlah)
                putString("kontak", layananItem.kontak)
                putString("tujuan", layananItem.tujuan)
                putString("filePath", layananItem.filePath)
            }

            "pemasangan" -> bundle.apply {
                putString("jenis", layananItem.jenis)
                putString("kontak", layananItem.kontak)
                putString("tujuan", layananItem.tujuan)
            }

            "pengaduan" -> bundle.apply {
                putString("layanan", layananItem.layanan)
                putString("kontak", layananItem.kontak)
                putString("keluhan", layananItem.keluhan)
                putString("filePath", layananItem.filePath)
            }

            "pembuatan" -> bundle.apply {
                putString("layanan", layananItem.layanan)
                putString("namaLayanan", layananItem.namaLayanan)
                putString("kontak", layananItem.kontak)
                putString("tujuan", layananItem.tujuan)
            }

            "lapor_kerusakan" -> bundle.apply {
                putString("namaPerangkat", layananItem.namaPerangkat)
                putString("kontak", layananItem.kontak)
                putString("keterangan", layananItem.keterangan)
                putString("imagePath", layananItem.imagePath)
            }

            else -> {
                Log.e("LayananAdapter", "Unknown formType: ${layananItem.formType}")
            }
        }

        return bundle
    }

    private fun getNavigationId(formType: String): Int? {
        return when (formType) {
            "pemeliharaan" -> R.id.action_historyLayananFragment_to_formPemeliharaanAkunFragment
            "bantuan" -> R.id.action_historyLayananFragment_to_formBantuanOperatorFragment
            "pemasangan" -> R.id.action_historyLayananFragment_to_formPemasanganPerangkatFragment
            "pengaduan" -> R.id.action_historyLayananFragment_to_formPengaduanLayananFragment
            "pembuatan" -> R.id.action_historyLayananFragment_to_formPembuatanWebDllFragment
            "lapor_kerusakan" -> R.id.action_historyLayananFragment_to_formLaporKerusakanFragment
            else -> null
        }
    }

    private fun handleUnknownFormType(view: View, layananItem: LayananItem) {
        Log.e("LayananAdapter", "Unknown formType: ${layananItem.formType}")
        Toast.makeText(
            view.context,
            "Error: Tipe form tidak dikenali (${layananItem.formType})",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun handleNavigationError(e: Exception, layananItem: LayananItem) {
        Log.e("LayananAdapter", "Navigation failed: ${e.message}", e)
        onEditItem(layananItem, layananList.indexOf(layananItem))
    }

    private fun setupSubmitButton(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        holder.btnBatalkan.setOnClickListener {
            val updatedItem = layananItem.copy(status = "Terkirim")
            layananList[position] = updatedItem
            notifyItemChanged(position)
            onStatusChanged(updatedItem, position)
        }
    }

    private fun setupBatalkanButton(
        holder: LayananViewHolder,
        context: Context,
        layananItem: LayananItem,
        position: Int
    ) {
        holder.btnBatalkan.setOnClickListener {
            showCancelConfirmationDialog(context, layananItem, position)
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, layananItem: LayananItem, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus layanan \"${layananItem.judul}\"?")
            .setPositiveButton("Ya") { dialog, _ ->
                handleDeleteConfirmation(context, layananItem, position)
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun handleDeleteConfirmation(context: Context, layananItem: LayananItem, position: Int) {
        layananList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, layananList.size)

        Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
        onDeleteItem(layananItem, position)
    }

    private fun showCancelConfirmationDialog(context: Context, layananItem: LayananItem, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Pembatalan")
            .setMessage("Apakah Anda yakin ingin membatalkan layanan \"${layananItem.judul}\"?")
            .setPositiveButton("Ya") { dialog, _ ->
                handleCancelConfirmation(context, layananItem, position)
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun handleCancelConfirmation(context: Context, layananItem: LayananItem, position: Int) {
        layananList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, layananList.size)

        Toast.makeText(context, "Layanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
        // Note: onDeleteItem is called here but could be renamed to onCancelItem for clarity
        onDeleteItem(layananItem, position)
    }

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

    fun resetSubmitButton(position: Int) {
        if (position in 0 until layananList.size) {
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = layananList.size
}