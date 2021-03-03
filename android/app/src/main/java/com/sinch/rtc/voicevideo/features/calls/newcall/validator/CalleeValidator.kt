package com.sinch.rtc.voicevideo.features.calls.newcall.validator

interface CalleeValidator {
    fun isCalleeValid(value: String): Boolean
}