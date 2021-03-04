package com.sinch.rtc.voicevideo.domain.calls

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class CallItem(
    val type: CallType,
    val callee: String,
    val startDate: Date
) : Parcelable