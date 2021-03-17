package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem

sealed class NewCallNavigationEvent

data class OutgoingCall(val call: CallItem) : NewCallNavigationEvent()
