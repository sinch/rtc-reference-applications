package com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator

import java.util.regex.Pattern

class AppDestinationValidator : DestinationValidator {

    private val appCalleePattern: Pattern = Pattern.compile(
        "^[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghjiklmnopqrstuvwxyz0123456789-_=]+$",
        Pattern.CASE_INSENSITIVE
    )

    override fun isCalleeValid(value: String): Boolean {
        return appCalleePattern.matcher(value).matches()
    }
}