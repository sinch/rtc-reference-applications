package com.sinch.rtc.voicevideo.features.calls.history.list

import android.view.View
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.databinding.ItemCallHistoryBinding
import com.sinch.rtc.voicevideo.domain.calls.CallHistoryItem
import com.sinch.rtc.voicevideo.domain.calls.CallType
import com.sinch.rtc.voicevideo.utils.sectionedrecycler.BaseItem
import java.text.DateFormat

data class CallHistoryEntryItem(
    val callHistoryItem: CallHistoryItem,
    val historyItemClickListener: HistoryItemClicksListener
) :
    BaseItem<ItemCallHistoryBinding> {

    interface HistoryItemClicksListener {
        fun onVideoClicked(item: CallHistoryItem)
        fun onAudioClicked(item: CallHistoryItem)
        fun onCalleeNameClicked(item: CallHistoryItem)
    }

    override val layoutId = R.layout.item_call_history
    override val uniqueId = callHistoryItem

    override fun initializeViewBinding(view: View): ItemCallHistoryBinding =
        ItemCallHistoryBinding.bind(view)

    override fun bind(binding: ItemCallHistoryBinding) {
        binding.apply {
            iconVideo.visibility =
                if (callHistoryItem.type == CallType.Video) View.VISIBLE else View.INVISIBLE
            iconVideo.setOnClickListener {
                historyItemClickListener.onVideoClicked(callHistoryItem)
            }

            calleeNameTextView.text = callHistoryItem.callee
            calleeNameTextView.setOnClickListener {
                historyItemClickListener.onCalleeNameClicked(callHistoryItem)
            }

            callTimeTextView.text = DateFormat.getTimeInstance().format(callHistoryItem.startDate)

            iconVoice.setOnClickListener {
                historyItemClickListener.onAudioClicked(callHistoryItem)
            }
        }
    }
}