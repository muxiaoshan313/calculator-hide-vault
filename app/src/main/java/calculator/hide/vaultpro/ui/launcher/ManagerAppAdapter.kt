package calculator.hide.vaultpro.ui.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vaultpro.R

class ManagerAppAdapter(
    private val onItemClick: (AppListItemUi) -> Unit
) : ListAdapter<AppListItemUi, ManagerAppAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manager_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.ivIcon)
        private val labelView: TextView = itemView.findViewById(R.id.tvLabel)
        private val packageView: TextView = itemView.findViewById(R.id.tvPackage)
        private val checkBox: CheckBox = itemView.findViewById(R.id.cbHidden)

        fun bind(item: AppListItemUi) {
            labelView.text = item.app.label.ifBlank { item.app.packageName }
            packageView.text = item.app.packageName
            checkBox.isChecked = item.isHidden
            iconView.setImageDrawable(
                item.icon ?: ContextCompat.getDrawable(
                    itemView.context,
                    R.mipmap.ic_launcher
                )
            )
            itemView.setOnClickListener { onItemClick(item) }
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
