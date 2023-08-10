package com.sinch.rtc.vvc.reference.app.application

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager

class RTCVoiceVideoReferenceApp : Application() {

    private val prefsManager: SharedPrefsManager by lazy {
        SharedPrefsManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                prefsManager.fcmRegistrationToken = it.result.orEmpty()
            }
        }
    }

}