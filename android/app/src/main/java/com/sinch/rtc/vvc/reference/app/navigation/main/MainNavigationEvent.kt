package com.sinch.rtc.vvc.reference.app.navigation.main

import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialAction

sealed class MainNavigationEvent

object Login : MainNavigationEvent()
object Dashboard : MainNavigationEvent()
data class IncomingCall(
    val callId: String,
    val initialAction: IncomingCallInitialAction) : MainNavigationEvent()