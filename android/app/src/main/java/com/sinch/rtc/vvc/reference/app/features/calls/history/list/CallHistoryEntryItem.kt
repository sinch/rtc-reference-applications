package com.sinch.rtc.vvc.reference.app.features.calls.history.list

import android.view.View
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.ItemCallHistoryBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.utils.sectionedrecycler.BaseItem
import java.text.DateFormat

data class CallHistoryEntryItem(
    val callItem: CallItem,
    val historyItemClickListener: HistoryItemClicksListener
) :
    BaseItem<ItemCallHistoryBinding> {

    interface HistoryItemClicksListener {
        fun onVideoClicked(item: CallItem)
        fun onAudioClicked(item: CallItem)
        fun onCalleeNameClicked(item: CallItem)
    }

    override val layoutId = R.layout.item_call_history
    override val uniqueId = callItem

    override fun initializeViewBinding(view: View): ItemCallHistoryBinding =
        ItemCallHistoryBinding.bind(view)

    override fun bind(binding: ItemCallHistoryBinding) {
        binding.apply {
            iconVideo.visibility =
                if (callItem.type == CallType.AppToAppVideo) View.VISIBLE else View.INVISIBLE
            iconVideo.setOnClickListener {
                historyItemClickListener.onVideoClicked(callItem)
            }

            destinationNameTextView.text = callItem.destination
            destinationNameTextView.setOnClickListener {
                historyItemClickListener.onCalleeNameClicked(callItem)
            }

            callTimeTextView.text = DateFormat.getTimeInstance().format(callItem.startDate)

            iconVoice.setOnClickListener {
                historyItemClickListener.onAudioClicked(callItem)
            }
        }
    }
}