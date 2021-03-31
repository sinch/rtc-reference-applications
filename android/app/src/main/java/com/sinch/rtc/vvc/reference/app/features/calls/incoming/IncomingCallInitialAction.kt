package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class IncomingCallInitialAction : Parcelable {
    NONE, ANSWER, DECLINE
}