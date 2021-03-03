package com.sinch.rtc.voicevideo.features.calls.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.domain.calls.CallHistoryItem

class CallHistoryAdapter(
    private val dataSet: List<CallHistoryItem>,
    private val listener: HistoryItemClicksListener
) :
    RecyclerView.Adapter<CallHistoryViewHolder>() {

    interface HistoryItemClicksListener {
        fun onVoiceClicked(item: CallHistoryItem)
        fun onAudioClicked(item: CallHistoryItem)
        fun onCalleeNameClicked(item: CallHistoryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder =
        CallHistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_call_history, parent, false
            )
        ).apply {
            voiceIconTextView.setOnClickListener {
                listener.onVoiceClicked(dataSet[adapterPosition])
            }
            videoIconTextView.setOnClickListener {
                listener.onAudioClicked(dataSet[adapterPosition])
            }
            calleeNameTextView.setOnClickListener {
                listener.onCalleeNameClicked(dataSet[adapterPosition])
            }
        }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

}