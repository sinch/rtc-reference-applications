package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IncomingCallInitialData(
    val callId: String,
    val initialAction: IncomingCallInitialAction) : Parcelable