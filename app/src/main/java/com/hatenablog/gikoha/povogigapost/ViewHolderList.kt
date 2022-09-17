package com.hatenablog.gikoha.povogigapost

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ViewHolderList(item: View) : RecyclerView.ViewHolder(item) {
    val dateField: TextView = item.findViewById(R.id.recDateField)
    val gigaField: TextView = item.findViewById(R.id.recGigaField)
    val memoField: TextView = item.findViewById(R.id.recMemoField)
}
