package com.sinch.rtc.voicevideo.features.calls.newcall.validator

import java.util.regex.Pattern

class SipCalleeValidator : CalleeValidator {

    private val sipPattern: Pattern = Pattern.compile(
        "^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$",
        Pattern.CASE_INSENSITIVE
    )

    override fun isCalleeValid(value: String): Boolean {
        return sipPattern.matcher(value).matches()
    }

}