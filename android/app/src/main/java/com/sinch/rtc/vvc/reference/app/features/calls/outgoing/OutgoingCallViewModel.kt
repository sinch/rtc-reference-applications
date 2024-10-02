package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.MissingPermissionException
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.CallState
import com.sinch.rtc.vvc.reference.app.application.service.NewCallHook
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.requiredPermissions
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.extensions.PermissionRequestResult
import com.sinch.rtc.vvc.reference.app.utils.extensions.areAllPermissionsGranted
import com.sinch.rtc.vvc.reference.app.utils.extensions.createSinchCall
import com.sinch.rtc.vvc.reference.app.utils.extensions.updateBasedOnSinchCall
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class OutgoingCallViewModel(
    private val callController: CallController,
    private val newCallHook: NewCallHook,
    private val prefsManager: SharedPrefsManager,
    private val callItem: CallItem,
    private val callDao: CallDao,
    application: Application
) :
    AndroidViewModel(application), CallListener {

    private val callStateMutable: MutableLiveData<CallState> = MutableLiveData()

    val navigationEvents: SingleLiveEvent<OutgoingCallNavigationEvent> = SingleLiveEvent()
    val permissionsRequiredEvent: SingleLiveEvent<List<String>> = SingleLiveEvent()

    val callState: LiveData<CallState> get() = callStateMutable
    val callItemLiveData: LiveData<CallItem> get() = MutableLiveData(callItem)

    private var sinchCall: Call? = null //We have to store it as user can cancel the call first

    companion object {
        const val TAG = "OutgoingCallViewModel"
    }

    fun onViewCreated() {
        if (sinchCall == null) {
            permissionsRequiredEvent.postValue(callItem.type.requiredPermissions)
        }
    }

    fun onPermissionsResult(permissionRequestResult: PermissionRequestResult) {
        if (sinchCall != null) {
            return
        }
        if (permissionRequestResult.areAllPermissionsGranted) {
            initializeCall()
        } else {
            navigationEvents.postValue(Back)
        }
    }

    fun onBackPressed() {
        finishCurrentCall()
    }

    fun onCancelButtonPressed() {
        finishCurrentCall()
        navigationEvents.postValue(Back)
    }

    private fun initializeCall() {
        try {
            sinchCall = callItem.createSinchCall(callController, prefsManager.usedConfig.cli)
                .apply { addCallListener(this@OutgoingCallViewModel) }
        } catch (e: MissingPermissionException) {
            permissionsRequiredEvent.postValue(callItem.type.requiredPermissions)
        }
        sinchCall?.let {
            newCallHook.onNewSinchCall(it)
            issueCallStateUpdate(it)
        }
    }

    override fun onCallProgressing(call: Call) {
        issueCallStateUpdate(call)
        Log.d(TAG, "onCallProgressing for $call")
    }

    override fun onCallRinging(call: Call) {
        super.onCallRinging(call)
        issueCallStateUpdate(call)
        Log.d(TAG, "onCallAnswered for $call")
    }

    override fun onCallAnswered(call: Call) {
        super.onCallAnswered(call)
        issueCallStateUpdate(call)
        Log.d(TAG, "onCallAnswered for $call")
    }

    override fun onCallEstablished(call: Call) {
        Log.d(TAG, "onCallEstablished for $call")
        issueCallStateUpdate(call)
        callItem.updateBasedOnSinchCall(call, callDao)
        navigationEvents.postValue(EstablishedCall(this.callItem, call.callId))
    }

    override fun onCallEnded(call: Call) {
        Log.d(TAG, "onCallEnded for $call")
        issueCallStateUpdate(call)
        callItem.updateBasedOnSinchCall(call, callDao)
        finishCurrentCall()
        navigationEvents.postValue(Back)
    }

    override fun onCleared() {
        super.onCleared()
        sinchCall?.removeCallListener(this)
    }

    private fun issueCallStateUpdate(call: Call) {
        callStateMutable.postValue(call.state)
    }

    private fun finishCurrentCall() {
        sinchCall?.hangup()
    }

}