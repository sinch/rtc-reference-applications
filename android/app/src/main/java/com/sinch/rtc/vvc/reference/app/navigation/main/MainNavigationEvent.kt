package com.sinch.rtc.vvc.reference.app.navigation.main

sealed class MainNavigationEvent

object Login : MainNavigationEvent()
object Dashboard : MainNavigationEvent()
data class IncomingCall(val callId: String) : MainNavigationEvent()