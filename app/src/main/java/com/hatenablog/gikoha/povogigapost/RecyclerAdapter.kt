package com.hatenablog.gikoha.povogigapost

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

data class PovoGiga (
    val date: String,
    val gigaleft: String,
    val memo: String?,
)

class RecyclerAdapter(val list: ArrayList<PovoGiga>) : RecyclerView.Adapter<ViewHolderList>()
{

    override fun onCreateViewHolder(parent: ViewGroup , viewType: Int): ViewHolderList {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_list, parent, false)
        return ViewHolderList(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolderList, position: Int) {
        holder.dateField.text = list[position].date
        holder.gigaField.text = list[position].gigaleft + " GB"
        holder.memoField.text = list[position].memo ?: ""
    }

    override fun getItemCount(): Int = list.size
}