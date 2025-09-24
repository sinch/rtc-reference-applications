package com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator

import java.util.regex.Pattern

class SipDestinationValidator : DestinationValidator {

    private val sipPattern: Pattern = Pattern.compile(
        "^(?:[^:]*(?::[^@]*)?@)?([^:@]+)(?::([0-9]*))?$",
        Pattern.CASE_INSENSITIVE
    )

    override fun isCalleeValid(value: String): Boolean {
        return sipPattern.matcher(value).matches()
    }

}