package com.sinch.rtc.vvc.reference.app.application.hms

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.sinch.android.rtc.SinchPush
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager

class HmsListenerService : HmsMessageService() {

    companion object {
        const val TAG = "HmsListenerService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived called")
        val data = remoteMessage.dataOfMap
        if (SinchPush.isSinchPushPayload(data)) {
            startService(Intent(this, SinchClientService::class.java))
            bindService(
                Intent(this, SinchClientService::class.java),
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (service is SinchClientService.SinchClientServiceBinder) {
                            service.sinchClient?.relayRemotePushNotification(
                                SinchPush.queryPushNotificationPayload(applicationContext, data)
                            )
                        }
                        unbindService(this)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}
                },
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken called")
        SharedPrefsManager(application).hmsRegistrationToken = token
    }
}
