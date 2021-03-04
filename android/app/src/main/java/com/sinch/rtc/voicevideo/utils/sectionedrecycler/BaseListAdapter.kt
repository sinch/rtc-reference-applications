package com.sinch.rtc.voicevideo.utils.sectionedrecycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

open class BaseListAdapter : ListAdapter<BaseItem<*>, BaseViewHolder<*>>(

    AsyncDifferConfig.Builder(object : DiffUtil.ItemCallback<BaseItem<*>>() {
        override fun areItemsTheSame(oldItem: BaseItem<*>, newItem: BaseItem<*>): Boolean {
            return oldItem.uniqueId == newItem.uniqueId
        }

        override fun areContentsTheSame(oldItem: BaseItem<*>, newItem: BaseItem<*>): Boolean {
            return oldItem == newItem
        }
    }).build()

) {
    private var lastItemForViewTypeLookup: BaseItem<*>? = null

    override fun getItemViewType(position: Int) = getItem(position).layoutId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        val item = getItemForViewType(viewType)
        return BaseViewHolder(item.initializeViewBinding(itemView))
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        getItem(position).bind(holder)
    }

    private fun getItemForViewType(viewType: Int): BaseItem<*> {
        val lastItemForViewTypeLookup = lastItemForViewTypeLookup
        if (lastItemForViewTypeLookup != null
            && lastItemForViewTypeLookup.layoutId == viewType
        ) {
            return lastItemForViewTypeLookup
        }

        for (i in 0 until itemCount) {
            val item: BaseItem<*> = getItem(i)
            if (item.layoutId == viewType) {
                return item
            }
        }
        throw IllegalStateException("Could not find model for view type: $viewType")
    }
}
