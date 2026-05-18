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

class AppListAdapter(
    private val loadIcon: ((LaunchableApp, (Drawable?) -> Unit) -> Unit)? = null,
    private val onItemClick: (AppListItemUi) -> Unit,
    private val onItemLongClick: (AppListItemUi) -> Boolean
) : ListAdapter<AppListItemUi, AppListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_drawer, parent, false)
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

        fun bind(item: AppListItemUi) {
            labelView.text = item.app.label.ifBlank { item.app.packageName }
            itemView.setTag(R.id.ivIcon, item.app.key)
            iconView.setImageDrawable(item.icon ?: placeholder)
            if (item.icon == null && loadIcon != null) {
                loadIcon(item.app) { drawable ->
                    if (itemView.getTag(R.id.ivIcon) == item.app.key) {
                        iconView.setImageDrawable(drawable ?: placeholder)
                    }
                }
            }
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppListItemUi>() {
        override fun areItemsTheSame(oldItem: AppListItemUi, newItem: AppListItemUi): Boolean {
            return oldItem.app.key == newItem.app.key
        }

        override fun areContentsTheSame(oldItem: AppListItemUi, newItem: AppListItemUi): Boolean {
            return oldItem == newItem
        }
    }
}
