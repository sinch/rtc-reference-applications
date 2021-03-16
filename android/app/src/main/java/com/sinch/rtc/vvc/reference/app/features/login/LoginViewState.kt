package com.sinch.rtc.vvc.reference.app.features.login

sealed class LoginViewState

object Idle : LoginViewState()

data class Logging(
    val username: String,
    val isUserRegistered: Boolean,
    val isPushTokenRegistered: Boolean
) : LoginViewState() {
    val isLoggingComplete: Boolean get() = isUserRegistered && isPushTokenRegistered
}