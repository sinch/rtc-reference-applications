package com.sinch.rtc.vvc.reference.app.navigation.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import com.sinch.android.rtc.calling.Call
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialData
import com.sinch.rtc.vvc.reference.app.utils.mvvm.ConsumableEventsLiveData
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class MainViewModel(
    private val app: Application,
    private val userDao: UserDao
) :
    AndroidViewModel(app), ServiceConnection {

    companion object {
        const val TAG = "MainViewModel"
    }

    private var initialCallIncomingCallInitialData: IncomingCallInitialData? = null

    val navigationEvents: SingleLiveEvent<MainNavigationEvent> = SingleLiveEvent()
    val serviceBound: SingleLiveEvent<Unit?> = SingleLiveEvent()

    var sinchClientServiceBinder: SinchClientService.SinchClientServiceBinder? = null
        private set
    var callEndedEvents: ConsumableEventsLiveData<Call>? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service is SinchClientService.SinchClientServiceBinder) {
            this.sinchClientServiceBinder = service
            this.callEndedEvents = service.callEndedEvents
            routeToDestination()
            serviceBound.call()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    private fun routeToDestination() {
        val loggedInUser = userDao.loadLoggedInUser()
        when {
            loggedInUser == null -> {
                navigationEvents.postValue(Login)
            }

            initialCallIncomingCallInitialData != null -> {
                routeToIncomingCall()
            }

            else -> {
                navigationEvents.postValue(Dashboard)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        app.unbindService(this)
    }

    fun onViewCreated() {
        if (sinchClientServiceBinder == null) {
            app.bindService(
                Intent(app, SinchClientService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
        } else {
            routeToDestination()
        }
    }

    fun onIncomingCallRequested(data: IncomingCallInitialData) {
        initialCallIncomingCallInitialData = data
        if (sinchClientServiceBinder != null) {
            routeToIncomingCall()
        }

    }

    private fun routeToIncomingCall() {
        initialCallIncomingCallInitialData?.let {
            navigationEvents.postValue(
                IncomingCall(
                    it.callId,
                    it.initialAction
                )
            )
        }
        initialCallIncomingCallInitialData = null
    }

}