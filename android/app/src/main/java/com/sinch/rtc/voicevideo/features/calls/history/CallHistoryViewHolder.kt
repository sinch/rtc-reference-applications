package com.sinch.rtc.voicevideo.features.calls.history

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.domain.calls.CallHistoryItem
import com.sinch.rtc.voicevideo.domain.calls.CallType
import java.text.DateFormat

class CallHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val voiceIconTextView: ImageView
    val videoIconTextView: ImageView
    val calleeNameTextView: TextView
    val callTimeTextView: TextView

    init {
        view.let {
            voiceIconTextView = it.findViewById(R.id.iconVoice)
            videoIconTextView = it.findViewById(R.id.iconVideo)
            calleeNameTextView = it.findViewById(R.id.calleeNameTextView)
            callTimeTextView = it.findViewById(R.id.callTimeTextView)
        }

    }

    fun bind(item: CallHistoryItem) {
        videoIconTextView.visibility =
            if (item.type == CallType.Video) View.VISIBLE else View.INVISIBLE
        calleeNameTextView.text = item.callee
        callTimeTextView.text = DateFormat.getTimeInstance().format(item.startDate)
    }

}