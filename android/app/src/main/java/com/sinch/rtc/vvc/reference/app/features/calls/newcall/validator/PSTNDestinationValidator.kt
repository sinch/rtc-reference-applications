package com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator

import android.telephony.PhoneNumberUtils

class PSTNDestinationValidator : DestinationValidator {

    override fun isCalleeValid(value: String): Boolean {
        return PhoneNumberUtils.isGlobalPhoneNumber(value)
    }

}