package com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator

class ConferenceDestinationValidator: DestinationValidator {
    override fun isCalleeValid(value: String): Boolean = value.isNotEmpty() && value.length <= 64
}