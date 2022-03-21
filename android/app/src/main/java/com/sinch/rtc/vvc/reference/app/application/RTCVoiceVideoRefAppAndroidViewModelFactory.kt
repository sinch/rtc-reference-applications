package com.sinch.rtc.vvc.reference.app.application

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavArgs
import com.sinch.android.rtc.SinchClient
import com.sinch.rtc.vvc.reference.app.features.calls.established.EstablishedCallFragmentArgs
import com.sinch.rtc.vvc.reference.app.features.calls.established.EstablishedCallViewModel
import com.sinch.rtc.vvc.reference.app.features.calls.history.CallHistoryViewModel
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallFragmentArgs
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallViewModel
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.NewCallFragmentArgs
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.NewCallViewModel
import com.sinch.rtc.vvc.reference.app.features.calls.outgoing.OutgoingCallFragmentArgs
import com.sinch.rtc.vvc.reference.app.features.calls.outgoing.OutgoingCallViewModel
import com.sinch.rtc.vvc.reference.app.features.login.LoginViewModel
import com.sinch.rtc.vvc.reference.app.features.settings.SettingsViewModel
import com.sinch.rtc.vvc.reference.app.navigation.main.MainViewModel
import com.sinch.rtc.vvc.reference.app.storage.RTCVoiceVideoAppDatabase
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.jwt.FakeJWTFetcher

typealias NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory = RTCVoiceVideoRefAppAndroidViewModelFactory<NavArgs>

class RTCVoiceVideoRefAppAndroidViewModelFactory<Args : NavArgs>(
    private val application: Application,
    private val args: Args? = null,
    private val sinchClient: SinchClient? = null,
) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val roomDatabase = RTCVoiceVideoAppDatabase.getDatabase(application)
        val prefsManager = SharedPrefsManager(application)
        return when (modelClass) {
            LoginViewModel::class.java -> {
                LoginViewModel(
                    application,
                    prefsManager,
                    FakeJWTFetcher(prefsManager),
                    roomDatabase.userDao(),
                    roomDatabase.callDao()
                ) as T
            }
            MainViewModel::class.java -> {
                MainViewModel(
                    application,
                    roomDatabase.userDao()
                ) as T
            }
            CallHistoryViewModel::class.java -> {
                CallHistoryViewModel(
                    application,
                    roomDatabase.userDao().loadLoggedInUser()!!,
                    roomDatabase.callDao()
                ) as T
            }
            SettingsViewModel::class.java -> {
                SettingsViewModel(
                    application,
                    roomDatabase.userDao(),
                    roomDatabase.callDao(),
                    prefsManager,
                    sinchClient
                ) as T
            }
            NewCallViewModel::class.java -> {
                NewCallViewModel(
                    (args as NewCallFragmentArgs).initialCallItem,
                    application,
                    prefsManager.usedConfig,
                    roomDatabase.userDao(),
                    roomDatabase.callDao()
                ) as T
            }
            OutgoingCallViewModel::class.java -> {
                val arguments = (args as OutgoingCallFragmentArgs)
                OutgoingCallViewModel(
                    sinchClient?.callClient!!,
                    arguments.callItemData,
                    roomDatabase.callDao(),
                    application
                ) as T
            }
            EstablishedCallViewModel::class.java -> {
                val arguments = (args as EstablishedCallFragmentArgs)
                EstablishedCallViewModel(
                    sinchClient!!,
                    roomDatabase.userDao().loadLoggedInUser()!!,
                    arguments.sinchCallId,
                    roomDatabase.callDao(),
                    arguments.callItemData,
                    application
                ) as T
            }
            IncomingCallViewModel::class.java -> {
                val arguments = (args) as IncomingCallFragmentArgs
                IncomingCallViewModel(
                    sinchClient!!,
                    arguments.initialAction,
                    arguments.callId,
                    application,
                    roomDatabase.callDao(),
                    roomDatabase.userDao().loadLoggedInUser()
                ) as T
            }
            else -> super.create(modelClass)
        }
    }

}