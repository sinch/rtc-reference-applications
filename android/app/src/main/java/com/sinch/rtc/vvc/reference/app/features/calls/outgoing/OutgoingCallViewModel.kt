package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.MissingPermissionException
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.requiredPermissions
import com.sinch.rtc.vvc.reference.app.utils.extensions.PermissionRequestResult
import com.sinch.rtc.vvc.reference.app.utils.extensions.areAllPermissionsGranted
import com.sinch.rtc.vvc.reference.app.utils.extensions.createSinchCall
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class OutgoingCallViewModel(
    private val callClient: CallClient,
    private val callItem: CallItem,
    application: Application
) :
    AndroidViewModel(application), CallListener {

    private val isCallProgressingMutable: MutableLiveData<Boolean> = MutableLiveData(false)

    val navigationEvents: SingleLiveEvent<OutgoingCallNavigationEvent> = SingleLiveEvent()
    val permissionsRequiredEvent: SingleLiveEvent<List<String>> = SingleLiveEvent()

    val isCallProgressing: LiveData<Boolean> get() = isCallProgressingMutable
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
            sinchCall = callItem.createSinchCall(callClient)
                .apply { addCallListener(this@OutgoingCallViewModel) }
        } catch (e: MissingPermissionException) {
            permissionsRequiredEvent.postValue(callItem.type.requiredPermissions)
        }
    }

    override fun onCallProgressing(call: Call?) {
        isCallProgressingMutable.value = true
        Log.d(TAG, "onCallProgressing for $call")
    }

    override fun onCallEstablished(call: Call?) {
        Log.d(TAG, "onCallEstablished for $call")
        isCallProgressingMutable.value = false
        if (call != null) {
            navigationEvents.postValue(EstablishedCall(this.callItem, call.callId))
        }
    }

    override fun onCallEnded(call: Call?) {
        Log.d(TAG, "onCallEnded for $call")
        finishCurrentCall()
        navigationEvents.postValue(Back)
    }

    override fun onCleared() {
        super.onCleared()
        sinchCall?.removeCallListener(this)
    }

    private fun finishCurrentCall() {
        isCallProgressingMutable.value = false
        sinchCall?.hangup()
    }

}