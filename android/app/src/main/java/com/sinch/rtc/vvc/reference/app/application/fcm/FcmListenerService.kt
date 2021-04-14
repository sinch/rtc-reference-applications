package com.sinch.rtc.vvc.reference.app.application.fcm

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
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
        if (SinchHelpers.isSinchPushPayload(remoteMessage.data) && !isVideoCallWithoutGrantedPermissions(
                remoteMessage
            )
        ) {
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

    private fun isVideoCallWithoutGrantedPermissions(remoteMessage: RemoteMessage): Boolean {
        val pushNotificationPayload =
            SinchHelpers.queryPushNotificationPayload(applicationContext, remoteMessage.data)
        if (pushNotificationPayload.isCall &&
            pushNotificationPayload.callResult.isVideoOffered &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(
                TAG, "You are receiving video call without granted required CAMERA permissions. " +
                        "Automatic establishing a connection in that case will be fixed ASAP (to resemble App-to-Audio flow.)"
            )
            return true
        }
        return false
    }


}