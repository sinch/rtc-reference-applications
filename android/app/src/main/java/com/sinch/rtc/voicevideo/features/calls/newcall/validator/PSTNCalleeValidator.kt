package com.sinch.rtc.voicevideo.features.calls.newcall.validator

import android.telephony.PhoneNumberUtils

class PSTNCalleeValidator : CalleeValidator {

    override fun isCalleeValid(value: String): Boolean {
        return PhoneNumberUtils.isGlobalPhoneNumber(value)
    }

}