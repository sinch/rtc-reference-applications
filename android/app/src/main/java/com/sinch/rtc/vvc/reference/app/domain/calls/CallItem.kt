package com.sinch.rtc.vvc.reference.app.domain.calls

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class CallItem(
    val type: CallType,
    val destination: String,
    val startDate: Date
) : Parcelable