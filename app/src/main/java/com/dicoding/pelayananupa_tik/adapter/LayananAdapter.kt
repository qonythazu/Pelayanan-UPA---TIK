package com.dicoding.pelayananupa_tik.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.backend.model.LayananItem

class LayananAdapter(private val layananList: List<LayananItem>) :
    RecyclerView.Adapter<LayananAdapter.LayananViewHolder>() {

    class LayananViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layananText: TextView = itemView.findViewById(R.id.textLayanan)
        val tanggalText: TextView = itemView.findViewById(R.id.textTanggal)
        val statusText: TextView = itemView.findViewById(R.id.textStatus)
        val btnMore: ImageView = itemView.findViewById(R.id.btn_more)
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
                        // TODO: Implementasi update
                        true
                    }
                    R.id.action_delete -> {
                        // TODO: Implementasi delete
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
}
