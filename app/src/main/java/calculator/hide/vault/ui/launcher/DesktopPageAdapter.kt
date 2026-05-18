package calculator.hide.vault.ui.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vault.R
import calculator.hide.vault.data.launcher.LauncherConstants

class DesktopPageAdapter(
    private val gridAdapterFactory: () -> DesktopGridAdapter
) : RecyclerView.Adapter<DesktopPageAdapter.PageViewHolder>() {

    private var pages: List<List<DesktopCellUi>> = emptyList()

    fun submitPages(pages: List<List<DesktopCellUi>>) {
        this.pages = pages
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desktop_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.rvDesktopGrid)
        private var boundAdapter: DesktopGridAdapter? = null

        fun bind(cells: List<DesktopCellUi>) {
            if (boundAdapter == null) {
                boundAdapter = gridAdapterFactory().also {
                    recyclerView.layoutManager = GridLayoutManager(
                        itemView.context,
                        LauncherConstants.GRID_COLUMNS
                    )
                    recyclerView.setHasFixedSize(true)
                    recyclerView.isNestedScrollingEnabled = false
                    recyclerView.adapter = it
                }
            }
            boundAdapter?.submitList(cells)
        }
    }
}
