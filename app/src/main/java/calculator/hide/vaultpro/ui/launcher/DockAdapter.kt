package calculator.hide.vaultpro.ui.launcher

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
import calculator.hide.vaultpro.R

class DockAdapter(
    private val onAppClick: (DockSlotUi) -> Unit,
    private val onAppLongClick: (DockSlotUi) -> Unit
) : ListAdapter<DockSlotUi, DockAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_slot, parent, false)
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.width = parent.width / 4
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

        fun bind(slot: DockSlotUi) {
            val app = slot.app
            if (app == null) {
                iconView.setImageDrawable(null)
                iconView.alpha = 0.15f
                labelView.text = ""
                content.setOnClickListener(null)
                content.setOnLongClickListener(null)
                return
            }
            iconView.alpha = 1f
            iconView.setImageDrawable(
                slot.icon ?: ContextCompat.getDrawable(
                    itemView.context,
                    R.mipmap.ic_launcher
                )
            )
            labelView.text = app.label.ifBlank { app.packageName }
            content.setOnClickListener { onAppClick(slot) }
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
