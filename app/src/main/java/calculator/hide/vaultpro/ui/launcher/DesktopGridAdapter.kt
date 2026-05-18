package calculator.hide.vaultpro.ui.launcher

import android.graphics.drawable.Drawable
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
import calculator.hide.vaultpro.data.launcher.LaunchableApp

class DesktopGridAdapter(
    private val loadIcon: (LaunchableApp, (Drawable?) -> Unit) -> Unit,
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
        private val placeholder =
            ContextCompat.getDrawable(itemView.context, R.mipmap.ic_launcher)

        fun bind(cell: DesktopCellUi) {
            val app = cell.app
            if (app == null) {
                iconView.setImageDrawable(null)
                iconView.alpha = 0f
                labelView.text = ""
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)
                itemView.setTag(R.id.ivIcon, null)
                return
            }
            iconView.alpha = 1f
            labelView.text = app.label.ifBlank { app.packageName }
            itemView.setTag(R.id.ivIcon, app.key)
            iconView.setImageDrawable(cell.icon ?: placeholder)
            if (cell.icon == null) {
                loadIcon(app) { drawable ->
                    if (itemView.getTag(R.id.ivIcon) == app.key) {
                        iconView.setImageDrawable(drawable ?: placeholder)
                    }
                }
            }
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
                oldItem.app?.key == newItem.app?.key
        }

        override fun areContentsTheSame(oldItem: DesktopCellUi, newItem: DesktopCellUi): Boolean {
            return oldItem.app?.key == newItem.app?.key && oldItem.icon != null == (newItem.icon != null)
        }
    }
}
