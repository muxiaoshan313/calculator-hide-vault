package calculator.hide.vaultpro.ui.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import calculator.hide.vaultpro.R
import calculator.hide.vaultpro.data.launcher.LauncherConstants

class DesktopPageAdapter(
    private val gridAdapterFactory: () -> DesktopGridAdapter
) : RecyclerView.Adapter<DesktopPageAdapter.PageViewHolder>() {

    private var pages: List<List<DesktopCellUi>> = emptyList()
    private val gridAdapters = mutableMapOf<Int, DesktopGridAdapter>()

    fun submitPages(pages: List<List<DesktopCellUi>>) {
        this.pages = pages
        gridAdapters.clear()
        notifyDataSetChanged()
    }

    fun getGridAdapter(position: Int): DesktopGridAdapter? = gridAdapters[position]

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_desktop_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position)
    }

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.rvDesktopGrid)

        fun bind(cells: List<DesktopCellUi>, pageIndex: Int) {
            val adapter = gridAdapters.getOrPut(pageIndex) { gridAdapterFactory() }
            recyclerView.layoutManager = GridLayoutManager(
                itemView.context,
                LauncherConstants.GRID_COLUMNS
            )
            recyclerView.adapter = adapter
            adapter.submitList(cells)
        }
    }
}
