package calculator.hide.vaultpro.ui.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vaultpro.R
class DesktopGridAdapter(
    private val onAppClick: (DesktopCellUi) -> Unit,
    private val onAppLongClick: (DesktopCellUi) -> Unit
) : ListAdapter<DesktopCellUi, DesktopGridAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desktop_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.ivIcon)
        private val labelView: TextView = itemView.findViewById(R.id.tvLabel)

        fun bind(cell: DesktopCellUi) {
            val app = cell.app
            if (app == null) {
                iconView.setImageDrawable(null)
                iconView.alpha = 0f
                labelView.text = ""
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)
                return
            }
            iconView.alpha = 1f
            iconView.setImageDrawable(
                cell.icon ?: ContextCompat.getDrawable(
                    itemView.context,
                    R.mipmap.ic_launcher
                )
            )
            labelView.text = app.label.ifBlank { app.packageName }
            itemView.setOnClickListener { onAppClick(cell) }
            itemView.setOnLongClickListener {
                onAppLongClick(cell)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DesktopCellUi>() {
        override fun areItemsTheSame(oldItem: DesktopCellUi, newItem: DesktopCellUi): Boolean {
            return oldItem.cellX == newItem.cellX &&
                oldItem.cellY == newItem.cellY &&
                oldItem.desktopId == newItem.desktopId
        }

        override fun areContentsTheSame(oldItem: DesktopCellUi, newItem: DesktopCellUi): Boolean {
            return oldItem == newItem
        }
    }
}
