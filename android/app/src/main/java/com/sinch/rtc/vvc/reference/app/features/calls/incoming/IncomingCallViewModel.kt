package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallEndCause
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.insertAndGetWithGeneratedId
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.CallProperties
import com.sinch.rtc.vvc.reference.app.domain.calls.requiredPermissions
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.utils.extensions.PermissionRequestResult
import com.sinch.rtc.vvc.reference.app.utils.extensions.areAllPermissionsGranted
import com.sinch.rtc.vvc.reference.app.utils.extensions.updateBasedOnSinchCall
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class IncomingCallViewModel(
    private val sinchClient: SinchClient,
    private val initialAction: IncomingCallInitialAction,
    private val callId: String,
    private val app: Application,
    private val callDao: CallDao,
    private val user: User?
) : AndroidViewModel(app), CallListener {

    private val call: Call
    private var callItem: CallItem? = null

    private val isCallProgressingMutable: MutableLiveData<Boolean> = MutableLiveData(false)
    private val callPropertiesMutable: MutableLiveData<CallProperties> = MutableLiveData()
    private val permissionsRequiredEvents: SingleLiveEvent<List<String>> = SingleLiveEvent()

    val navigationEvents: SingleLiveEvent<IncomingCallNavigationEvent> = SingleLiveEvent()
    val callProperties: LiveData<CallProperties> = callPropertiesMutable
    val isCallProgressing: LiveData<Boolean> get() = isCallProgressingMutable
    val permissionsEvents: LiveData<List<String>> get() = permissionsRequiredEvents

    companion object {
        const val TAG = "IncomingCallViewModel"
    }

    init {
        Log.d(TAG, "IncomingCallViewModel initialised with $callId")
        call = sinchClient.callClient.getCall(callId).apply {
            addCallListener(this@IncomingCallViewModel)
        }
        callPropertiesMutable.postValue(CallProperties(call.remoteUserId))
        isCallProgressingMutable.postValue(true)
        user?.let {
            val generatedCallItem = CallItem(call = call, user = it).let { item ->
                callDao.insertAndGetWithGeneratedId(item)
            }
            callItem = generatedCallItem
        }
        handleImmediateResponse()
    }

    fun onCallAccepted() {
        acceptCall()
    }

    fun onBackPressed() {
        declineCall()
    }

    fun onPermissionsResult(permissionRequestResult: PermissionRequestResult) {
        if (permissionRequestResult.areAllPermissionsGranted) {
            call.answer()
            callItem?.let {
                navigationEvents.value = EstablishedCall(it, callId)
            }
        } else {
            declineCall()
            navigationEvents.value = Back
        }
    }

    override fun onCallProgressing(call: Call?) {
        Log.d(TAG, "onCallProgressing for $call")
    }

    override fun onCallEstablished(call: Call?) {
        Log.d(TAG, "onCallProgressing for $call")
        callItem?.updateBasedOnSinchCall(call, callDao)
    }

    override fun onCallEnded(call: Call?) {
        Log.d(TAG, "Call ended with end cause ${call?.details?.endCause}")
        callItem?.updateBasedOnSinchCall(call, callDao)
        if (call?.details?.endCause != CallEndCause.DENIED) { //Not initiated by the user
            navigationEvents.postValue(Back)
        }
    }

    override fun onCleared() {
        super.onCleared()
        call.removeCallListener(this)
    }

    private fun acceptCall() {
        isCallProgressingMutable.postValue(false)
        callItem?.type?.requiredPermissions.let {
            permissionsRequiredEvents.value = it
        }
    }

    private fun declineCall() {
        call.hangup()
        isCallProgressingMutable.postValue(false)
    }

    private fun handleImmediateResponse() {
        when (initialAction) {
            IncomingCallInitialAction.ANSWER -> acceptCall()
            IncomingCallInitialAction.DECLINE -> navigationEvents.value = Back
            IncomingCallInitialAction.NONE -> return
        }
    }
}