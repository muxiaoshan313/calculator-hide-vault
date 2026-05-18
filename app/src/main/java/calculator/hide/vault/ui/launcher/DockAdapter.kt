package calculator.hide.vault.ui.launcher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R
import calculator.hide.vault.data.launcher.LaunchableApp
import calculator.hide.vault.data.launcher.LauncherConstants
import calculator.hide.vault.data.launcher.LauncherItemType

class DockAdapter(
    private val loadIcon: ((LaunchableApp, (Drawable?) -> Unit) -> Unit)? = null,
    private val onSlotClick: (DockSlotUi) -> Unit,
    private val onAppLongClick: (DockSlotUi) -> Unit
) : ListAdapter<DockSlotUi, DockAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_slot, parent, false)
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.width = (parent.width / LauncherConstants.DOCK_SIZE).coerceAtLeast(1)
        view.layoutParams = lp
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: LinearLayout = itemView.findViewById(R.id.dockContent)
        private val iconView: ImageView = itemView.findViewById(R.id.ivDockIcon)
        private val labelView: TextView = itemView.findViewById(R.id.tvDockLabel)
        private val placeholder =
            ContextCompat.getDrawable(itemView.context, R.mipmap.ic_launcher)

        fun bind(slot: DockSlotUi) {
            when (slot.itemType) {
                LauncherItemType.ALL_APPS_ENTRY -> {
                    iconView.alpha = 1f
                    iconView.setImageResource(R.drawable.ic_launcher_all_apps)
                    labelView.text = slot.title ?: itemView.context.getString(R.string.all_apps)
                    content.setOnClickListener { onSlotClick(slot) }
                    content.setOnLongClickListener(null)
                }
                LauncherItemType.PRIVATE_SPACE_ENTRY -> {
                    iconView.alpha = 1f
                    iconView.setImageResource(R.drawable.ic_launcher_private_space)
                    labelView.text = slot.title ?: itemView.context.getString(R.string.private_space)
                    content.setOnClickListener { onSlotClick(slot) }
                    content.setOnLongClickListener(null)
                }
                else -> bindAppSlot(slot)
            }
        }

        private fun bindAppSlot(slot: DockSlotUi) {
            val app = slot.app
            if (app == null) {
                iconView.setImageDrawable(null)
                iconView.alpha = 0.15f
                labelView.text = ""
                content.setTag(R.id.ivDockIcon, null)
                content.setOnClickListener(null)
                content.setOnLongClickListener(null)
                return
            }
            iconView.alpha = 1f
            labelView.text = app.label.ifBlank { app.packageName }
            content.setTag(R.id.ivDockIcon, app.key)
            iconView.setImageDrawable(slot.icon ?: placeholder)
            if (slot.icon == null && loadIcon != null) {
                loadIcon(app) { drawable ->
                    if (content.getTag(R.id.ivDockIcon) == app.key) {
                        iconView.setImageDrawable(drawable ?: placeholder)
                    }
                }
            }
            content.setOnClickListener { onSlotClick(slot) }
            content.setOnLongClickListener {
                onAppLongClick(slot)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DockSlotUi>() {
        override fun areItemsTheSame(oldItem: DockSlotUi, newItem: DockSlotUi): Boolean {
            return oldItem.position == newItem.position
        }

        override fun areContentsTheSame(oldItem: DockSlotUi, newItem: DockSlotUi): Boolean {
            return oldItem == newItem
        }
    }

}
