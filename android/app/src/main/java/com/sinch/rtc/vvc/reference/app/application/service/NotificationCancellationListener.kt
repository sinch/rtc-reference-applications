package com.sinch.rtc.vvc.reference.app.application.service

import android.app.NotificationManager
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener

class NotificationCancellationListener(private val notificationManager: NotificationManager) :
    CallListener {

    override fun onCallProgressing(p0: Call?) {}

    override fun onCallEstablished(p0: Call?) {
        notificationManager.cancelAll()
    }

    override fun onCallEnded(p0: Call?) {
        notificationManager.cancelAll()
    }

}