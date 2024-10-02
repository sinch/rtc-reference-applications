package com.sinch.rtc.vvc.reference.app.application.service

import com.sinch.android.rtc.calling.Call

fun interface NewCallHook {
    fun onNewSinchCall(call: Call)
}