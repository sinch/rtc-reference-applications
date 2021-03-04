package com.sinch.rtc.vvc.reference.app.utils.sectionedrecycler

import android.view.View
import androidx.viewbinding.ViewBinding

interface BaseItem<T : ViewBinding> {

    val layoutId: Int
    val uniqueId: Any

    fun initializeViewBinding(view: View): T

    fun bind(
        holder: BaseViewHolder<*>
    ) {
        val specificHolder = holder as BaseViewHolder<T>
        bind(specificHolder.binding)
    }

    fun bind(binding: T)

    override fun equals(other: Any?): Boolean

}