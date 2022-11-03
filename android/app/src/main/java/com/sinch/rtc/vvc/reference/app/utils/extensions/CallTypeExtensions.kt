package com.sinch.rtc.vvc.reference.app.utils.extensions

import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallDetails
import com.sinch.android.rtc.calling.MediaConstraints
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType

fun CallItem.createSinchCall(callController: CallController, cli: String?): Call = when (type) {
    CallType.AppToPhone -> callController.callPhoneNumber(destination, cli.orEmpty())
    CallType.AppToAppAudio -> callController.callUser(destination, MediaConstraints(false))
    CallType.AppToAppVideo -> callController.callUser(destination, MediaConstraints(true))
    CallType.AppToSip -> callController.callSip(destination)
}

val CallDetails.expectedType: CallType
    get() = if (this.isVideoOffered)
        CallType.AppToAppVideo else CallType.AppToAppAudio

fun CallItem.updateBasedOnSinchCall(call: Call?, dao: CallDao) {
    if (call != null) {
        dao.update(this.withUpdatedCallData(call))
    }
}