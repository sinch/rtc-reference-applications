package com.sinch.rtc.voicevideo.domain.calls

import java.util.*

data class CallHistoryItem(
    val type: CallType,
    val callee: String,
    val startDate: Date
)