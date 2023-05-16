package com.sinch.rtc.vvc.reference.app.application.service

import android.app.NotificationManager
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener

class NotificationCancellationListener(private val notificationManager: NotificationManager) :
    CallListener {

    override fun onCallProgressing(call: Call) {}

    override fun onCallEstablished(call: Call) {
        notificationManager.cancelAll()
    }

    override fun onCallEnded(call: Call) {
        notificationManager.cancelAll()
    }

}