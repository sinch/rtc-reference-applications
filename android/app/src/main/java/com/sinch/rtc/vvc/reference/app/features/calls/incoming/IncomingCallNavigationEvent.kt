package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem

sealed class IncomingCallNavigationEvent

object Back : IncomingCallNavigationEvent()
data class EstablishedCall(val callItem: CallItem, val sinchCallId: String) :
    IncomingCallNavigationEvent()
