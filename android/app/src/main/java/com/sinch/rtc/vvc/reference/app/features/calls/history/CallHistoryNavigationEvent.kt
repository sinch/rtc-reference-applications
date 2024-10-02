package com.sinch.rtc.vvc.reference.app.features.calls.history

import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem

sealed class CallHistoryNavigationEvent

data class OutGoingCall(val callItem: CallItem) : CallHistoryNavigationEvent()
data class NewCall(val callItem: CallItem) : CallHistoryNavigationEvent()
data class Details(val callItem: CallItem) : CallHistoryNavigationEvent()
