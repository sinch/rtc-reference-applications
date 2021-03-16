package com.sinch.rtc.vvc.reference.app.application

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sinch.rtc.vvc.reference.app.features.login.LoginViewModel
import com.sinch.rtc.vvc.reference.app.navigation.loggedin.LoggedInViewModel
import com.sinch.rtc.vvc.reference.app.storage.RTCVoiceVideoAppDatabase
import com.sinch.rtc.vvc.reference.app.utils.jwt.FakeJWTFetcher

class RTCVoiceVideoRefAppAndroidViewModelFactory(private val application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            LoginViewModel::class.java -> {
                LoginViewModel(
                    application,
                    FakeJWTFetcher(),
                    RTCVoiceVideoAppDatabase.getDatabase(application).wordDao()
                ) as T
            }
            LoggedInViewModel::class.java -> {
                LoggedInViewModel(
                    application,
                    RTCVoiceVideoAppDatabase.getDatabase(application).wordDao()
                ) as T
            }
            else -> super.create(modelClass)
        }
    }

}