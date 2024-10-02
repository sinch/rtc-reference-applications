package com.sinch.rtc.vvc.reference.app.application.service

import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.utils.mvvm.ConsumableEventsLiveData

class CallEndedConsumableEventsListener(private val consumableEventsLiveData: ConsumableEventsLiveData<Call>) :
    CallListener {

    override fun onCallEnded(call: Call) {
        consumableEventsLiveData.postData(call)
    }

    override fun onCallEstablished(call: Call) {}

    override fun onCallProgressing(call: Call) {}

}