package com.sinch.rtc.vvc.reference.app.application

import android.app.Application
import android.app.ActivityManager
import android.os.Process
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.agconnect.AGConnectInstance
import com.huawei.hms.aaid.HmsInstanceId
import com.sinch.rtc.vvc.reference.app.BuildConfig
import com.sinch.rtc.vvc.reference.app.domain.push.PushProvider
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RTCVoiceVideoReferenceApp : Application() {

    companion object {
        const val TAG = "RTCVVCRefApp"
    }

    private val prefsManager: SharedPrefsManager by lazy {
        SharedPrefsManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        // HMS Push SDK runs in a separate :pushservice process, which triggers Application.onCreate().
        // Firebase and AGConnect are not initialized there, so skip token prefetch outside the main process.
        if (!isMainProcess()) return

        if (BuildConfig.FCM_AVAILABLE) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener {
                    if (it.isSuccessful) {
                        prefsManager.fcmRegistrationToken = it.result.orEmpty()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM token prefetch failed", e)
            }
        }
        if (BuildConfig.HMS_AVAILABLE) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appId = AGConnectInstance.getInstance().options.getString("client/app_id")
                    val token = HmsInstanceId.getInstance(this@RTCVoiceVideoReferenceApp)
                        .getToken(appId, "HCM")
                    if (!token.isNullOrEmpty()) {
                        prefsManager.hmsRegistrationToken = token
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "HMS token prefetch failed", e)
                }
            }
        }
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses?.firstOrNull { it.pid == pid }
            ?.processName == packageName
    }
    
}
