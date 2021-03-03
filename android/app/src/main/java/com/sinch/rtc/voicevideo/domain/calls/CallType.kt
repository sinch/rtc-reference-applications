package com.sinch.rtc.voicevideo.domain.calls

import android.content.Context
import com.sinch.rtc.voicevideo.R

enum class CallType {
    PSTN,
    Audio,
    Video,
    SIP;
}

fun CallType.newCallLabel(context: Context): String {
    val resource = when (this) {
        CallType.PSTN -> R.string.call_pstn
        CallType.Audio -> R.string.call_app
        CallType.Video -> R.string.call_video
        CallType.SIP -> R.string.call_sip
    }
    return context.getString(resource)

}