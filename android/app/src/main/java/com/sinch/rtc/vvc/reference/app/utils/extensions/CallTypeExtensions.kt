package com.sinch.rtc.vvc.reference.app.utils.extensions

import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallDetails
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType

fun CallItem.createSinchCall(callClient: CallClient): Call = when (type) {
    CallType.AppToPhone -> callClient.callPhoneNumber(destination)
    CallType.AppToAppAudio -> callClient.callUser(destination)
    CallType.AppToAppVideo -> callClient.callUserVideo(destination)
    CallType.AppToSip -> callClient.callSip(destination)
}

val CallDetails.expectedType: CallType
    get() = if (this.isVideoOffered)
        CallType.AppToAppVideo else CallType.AppToAppAudio