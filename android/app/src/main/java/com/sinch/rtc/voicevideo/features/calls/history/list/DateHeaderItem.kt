package com.sinch.rtc.voicevideo.features.calls.history.list

import android.view.View
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.databinding.ItemCallHistoryDateHeaderBinding
import com.sinch.rtc.voicevideo.utils.sectionedrecycler.BaseItem

data class DateHeaderItem(val dateString: String) : BaseItem<ItemCallHistoryDateHeaderBinding> {

    override val layoutId = R.layout.item_call_history_date_header
    override val uniqueId = dateString

    override fun initializeViewBinding(view: View): ItemCallHistoryDateHeaderBinding =
        ItemCallHistoryDateHeaderBinding.bind(view)

    override fun bind(binding: ItemCallHistoryDateHeaderBinding) {
        binding.dateText.text = dateString
    }

}