package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem

sealed class OutgoingCallNavigationEvent

object Back : OutgoingCallNavigationEvent()

data class EstablishedCall(val callItem: CallItem, val sinchCallId: String) :
    OutgoingCallNavigationEvent()