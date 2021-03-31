package com.sinch.rtc.vvc.reference.app.application.fcm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sinch.android.rtc.SinchHelpers
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService

class FcmListenerService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FcmListenerService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived called with $remoteMessage")
        if (SinchHelpers.isSinchPushPayload(remoteMessage.data)) {
            startService(Intent(this, SinchClientService::class.java))
            bindService(
                Intent(this, SinchClientService::class.java),
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (service is SinchClientService.SinchClientServiceBinder) {
                            service.sinchClient?.relayRemotePushNotificationPayload(remoteMessage.data)
                        }
                        unbindService(this)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}

                },
                Context.BIND_AUTO_CREATE
            )
        }
    }

}