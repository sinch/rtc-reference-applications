package com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator

interface DestinationValidator {
    fun isCalleeValid(value: String): Boolean
}